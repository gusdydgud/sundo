package com.example.liststart

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import com.example.liststart.service.TurbineAPIService
import com.example.liststart.model.Business
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TurbineAPIServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var turbineApiService: TurbineAPIService

    @Before
    fun setUp() {
        // MockWebServer 시작
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Retrofit 인스턴스 설정 (MockWebServer URL 사용)
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/")) // MockWebServer URL 사용
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        turbineApiService = retrofit.create(TurbineAPIService::class.java)
    }

    @After
    fun tearDown() {
        // MockWebServer 종료
        mockWebServer.shutdown()
    }

    @Test
    fun getBusinessAllTest() {
        // Mock 서버 응답 설정 (JSON 응답)
        mockWebServer.enqueue(MockResponse().setBody("[{\"bno\":1,\"title\":\"test1\"}, {\"bno\":2,\"title\":\"test2\"}]"))

        // 동기 호출
        val call: Call<ArrayList<Business>> = turbineApiService.getBusinessAll()
        val response: Response<ArrayList<Business>> = call.execute()

        // 서버 응답이 성공적이었는지 확인
        assertTrue(response.isSuccessful)

        // 응답 데이터 확인
        val data = response.body()
        assertNotNull(data)
        assertEquals(2, data?.size)
        assertEquals("test1", data?.get(0)?.title)
        assertEquals("test2", data?.get(1)?.title)
    }
}
