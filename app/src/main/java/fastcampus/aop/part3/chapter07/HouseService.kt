package fastcampus.aop.part3.chapter07

import retrofit2.Call
import retrofit2.http.GET

// get items라서 Dto로 맵핑
interface HouseService {
    @GET("/v3/d671725e-6a6e-48f3-8cbe-489678427a7b")
    fun getHouseList(): Call<HouseDto>
}