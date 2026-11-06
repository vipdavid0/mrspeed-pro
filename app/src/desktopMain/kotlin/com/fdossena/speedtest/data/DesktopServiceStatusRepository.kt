package com.fdossena.speedtest.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class DesktopServiceStatusRepository(
    private val syncManager: SyncProvider
) : ServiceStatusRepository {
    private val _statuses = MutableStateFlow<List<ServiceStatus>>(emptyList())
    private val _isSyncing = MutableStateFlow(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    override fun getServiceStatuses(): Flow<List<ServiceStatus>> = _statuses.asStateFlow()
    override val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()
    @Volatile override var isTesting: Boolean = false

    private var isRefreshing = false

    init {
        loadInitialLocalFallback()
        scope.launch {
            while (isActive) {
                if (!isRefreshing && !isTesting) {
                    isRefreshing = true
                    try {
                        refreshStatuses()
                    } finally {
                        isRefreshing = false
                    }
                }
                delay(45000) // Slightly longer delay
            }
        }
    }

    private fun loadInitialLocalFallback() {
        try {
            val stream = javaClass.classLoader.getResourceAsStream("top_500_services.json")
                ?: javaClass.classLoader.getResourceAsStream("assets/top_500_services.json")
                ?: javaClass.classLoader.getResourceAsStream("resources/top_500_services.json")
            if (stream != null) {
                val text = stream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val fallbackList = mutableListOf<ServiceStatus>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.optString("name", "Service")
                    val cat = obj.optString("category", "Web Service")
                    val url = obj.optString("url", "https://${name.lowercase().replace(" ", "")}.com")
                    val statusStr = obj.optString("status", "UP")
                    val reports = obj.optInt("reports", obj.optInt("reportCount", 0))
                    fallbackList.add(ServiceStatus(
                        id = obj.optString("id", name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }),
                        name = name,
                        url = url,
                        statusUrl = obj.optString("statusUrl", url),
                        category = cat,
                        currentStatus = when(statusStr.uppercase()) {
                            "DOWN" -> StatusLevel.DOWN
                            "ISSUES" -> StatusLevel.ISSUES
                            else -> StatusLevel.UP
                        },
                        reportCount = reports,
                        historicalReports = emptyList()
                    ))
                }
                if (fallbackList.isNotEmpty()) {
                    _statuses.value = fallbackList
                    println("[DEBUG] DetectorRepo: Initialized ${fallbackList.size} local fallback services.")
                }
            }
        } catch (e: Exception) {
            println("[DEBUG] DetectorRepo: Error loading local fallback: ${e.message}")
        }
    }

    private suspend fun refreshStatuses() {
        if (isTesting) return
        println("[DEBUG] DetectorRepo: Fetching all service statuses from cloud...")
        try {
            _isSyncing.value = true
            val newList = mutableListOf<ServiceStatus>()
            var lastDocument: JSONObject? = null
            var hasMore = true
            
            while (hasMore && !isTesting) {
                val query = JSONObject().apply {
                    put("structuredQuery", JSONObject().apply {
                        put("from", JSONArray().put(JSONObject().put("collectionId", "status_services")))
                        put("orderBy", JSONArray().apply {
                            put(JSONObject().apply {
                                put("field", JSONObject().put("fieldPath", "reportCount"))
                                put("direction", "DESCENDING")
                            })
                            put(JSONObject().apply {
                                put("field", JSONObject().put("fieldPath", "__name__"))
                                put("direction", "DESCENDING")
                            })
                        })
                        lastDocument?.let { doc ->
                            val fields = doc.getJSONObject("fields")
                            val countStr = fields.optJSONObject("reportCount")?.optString("integerValue")
                                ?: fields.optJSONObject("reportCount")?.optInt("integerValue")?.toString()
                                ?: "0"
                            val docName = doc.getString("name")
                            
                            put("startAt", JSONObject().apply {
                                put("values", JSONArray().apply {
                                    put(JSONObject().put("integerValue", countStr))
                                    put(JSONObject().put("referenceValue", docName))
                                })
                                put("before", false)
                            })
                        }
                        put("limit", 100)
                    })
                }
                
                val response = syncManager.performFirestoreRequest("documents:runQuery", "POST", query.toString())
                if (response != null) {
                    val results = JSONArray(response)
                    
                    var addedInThisBatch = 0
                    for (i in 0 until results.length()) {
                        val resultObj = results.getJSONObject(i)
                        val doc = resultObj.optJSONObject("document") ?: continue
                        
                        val docId = doc.getString("name").substringAfterLast("/")
                        if (newList.any { it.id == docId }) continue
                        
                        val fields = doc.optJSONObject("fields") ?: continue
                        val name = fields.optJSONObject("name")?.optString("stringValue") ?: "Unknown"
                        
                        newList.add(ServiceStatus(
                            id = docId,
                            name = name,
                            url = fields.optJSONObject("url")?.optString("stringValue") ?: "",
                            statusUrl = fields.optJSONObject("statusUrl")?.optString("stringValue"),
                            category = fields.optJSONObject("category")?.optString("stringValue") ?: "Other",
                            currentStatus = when(fields.optJSONObject("currentStatus")?.optString("stringValue")) {
                                "DOWN" -> StatusLevel.DOWN
                                "ISSUES" -> StatusLevel.ISSUES
                                else -> StatusLevel.UP
                            },
                            reportCount = fields.optJSONObject("reportCount")?.optString("integerValue")?.toIntOrNull() 
                                ?: fields.optJSONObject("reportCount")?.optInt("integerValue") 
                                ?: 0,
                            historicalReports = emptyList()
                        ))
                        lastDocument = doc
                        addedInThisBatch++
                    }
                    
                    if (addedInThisBatch == 0 || results.length() < 100) {
                        hasMore = false
                    }
                    if (newList.size > 3000) hasMore = false // Safety cap
                } else {
                    hasMore = false
                }
                delay(100)
            }
            
            if (newList.isNotEmpty()) {
                _statuses.value = newList
                println("[DEBUG] DetectorRepo: Successfully loaded ${newList.size} services. Top rank: ${newList.firstOrNull()?.name}")
            }
            
        } catch (e: Exception) {
            println("[DEBUG] DetectorRepo: Error fetching statuses: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    override suspend fun addCustomService(name: String, url: String) {
        val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
        val fields = JSONObject().apply {
            put("name", JSONObject().put("stringValue", name))
            put("url", JSONObject().put("stringValue", url))
            put("reportCount", JSONObject().put("integerValue", "0"))
            put("currentStatus", JSONObject().put("stringValue", "UP"))
            put("category", JSONObject().put("stringValue", "User Added"))
            put("isCustom", JSONObject().put("booleanValue", true))
        }
        val body = JSONObject().put("fields", fields).toString()
        syncManager.performFirestoreRequest("status_services/$id", "PATCH", body)
        refreshStatuses()
    }

    override suspend fun reportIssue(serviceId: String) {
        println("[DEBUG] DetectorRepo: Reporting issue for $serviceId...")
        try {
            val currentResponse = syncManager.performFirestoreRequest("status_services/$serviceId", "GET")
            if (currentResponse != null) {
                val doc = JSONObject(currentResponse)
                val fields = doc.getJSONObject("fields")
                val count = fields.optJSONObject("reportCount")?.optString("integerValue")?.toIntOrNull() 
                    ?: fields.optJSONObject("reportCount")?.optInt("integerValue") 
                    ?: 0
                
                val updates = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("reportCount", JSONObject().put("integerValue", (count + 1).toString()))
                        put("currentStatus", JSONObject().put("stringValue", "ISSUES"))
                    })
                }
                syncManager.performFirestoreRequest("status_services/$serviceId?updateMask.fieldPaths=reportCount&updateMask.fieldPaths=currentStatus", "PATCH", updates.toString())
                refreshStatuses()
            }
        } catch (e: Exception) {
            println("[DEBUG] DetectorRepo: Report failed: ${e.message}")
        }
    }

    override suspend fun seedInitialServices(jsonString: String?) {
        if (_statuses.value.size < 5 && jsonString != null) {
            println("[DEBUG] DetectorRepo: Cloud empty or very low count, seeding from local assets...")
            try {
                val element = json.parseToJsonElement(jsonString)
                val jsonArray = when (element) {
                    is JsonArray -> element
                    is JsonObject -> {
                        element.values.firstOrNull { it is JsonArray } as? JsonArray ?: JsonArray(emptyList())
                    }
                    else -> JsonArray(emptyList())
                }
                
                val list = jsonArray.map {
                    val obj = it.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown"
                    val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
                    ServiceStatus(
                        id = id,
                        name = name,
                        url = obj["url"]?.jsonPrimitive?.content ?: "",
                        statusUrl = obj["statusUrl"]?.jsonPrimitive?.content,
                        category = obj["category"]?.jsonPrimitive?.content ?: "Other",
                        currentStatus = StatusLevel.UP,
                        reportCount = 0,
                        historicalReports = emptyList()
                    )
                }
                
                scope.launch {
                    list.forEach { svc ->
                        if (_statuses.value.none { it.id == svc.id }) {
                            addCustomServiceSilent(svc.name, svc.url)
                            delay(150)
                        }
                    }
                    refreshStatuses()
                }
                
                _statuses.value = list
            } catch (e: Exception) {}
        }
    }

    private suspend fun addCustomServiceSilent(name: String, url: String) {
        val id = name.lowercase().replace(" ", "").filter { it.isLetterOrDigit() }
        val fields = JSONObject().apply {
            put("name", JSONObject().put("stringValue", name))
            put("url", JSONObject().put("stringValue", url))
            put("reportCount", JSONObject().put("integerValue", "0"))
            put("currentStatus", JSONObject().put("stringValue", "UP"))
            put("category", JSONObject().put("stringValue", "User Added"))
            put("isCustom", JSONObject().put("booleanValue", true))
        }
        val body = JSONObject().put("fields", fields).toString()
        syncManager.performFirestoreRequest("status_services/$id", "PATCH", body)
    }
}
