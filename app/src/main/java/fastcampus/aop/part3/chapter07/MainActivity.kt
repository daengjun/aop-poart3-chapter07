package fastcampus.aop.part3.chapter07

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 네이버 지도 API 사용하기
 * 우리집 위치에 마커 찍어보기
 * 지도 위에 BottomSheetDialog 띄우기
 * Retrofit을 사용하여 서버에서 가져온 예약가능 목록 보여주기
 * 지도 위에 예약가능 집 목록 띄우기
 * BottomSheetDialog에 예약가능 집 목록 띄우기
 * 마커와 리스트 연동하기
 * 공유하기 기능 구현하기
 *
 */

// 추가적으로 딥링크를 이용해서 사용자가 메시지를 클릭했을때 해당 게시물을 보여준다거나, 실제 서버에서 데이터값을 받아온다던가 하는 부분이 있으면 좀더 앱 완성도가 높아질 것

class MainActivity : AppCompatActivity(), OnMapReadyCallback, Overlay.OnClickListener {

    // 네이버 맵 api
    private lateinit var naverMap: NaverMap
    // 현재 좌표 가져올때 사용
    private lateinit var locationSource: FusedLocationSource

    // 맵뷰 초기화 (지도 표시)
    private val mapView: MapView by lazy {
        findViewById(R.id.mapView)
    }

    // 뷰페이저 초기화
    private val viewPager: ViewPager2 by lazy {
        findViewById(R.id.houseViewPager)
    }

    // 리사이클러뷰 초기화
    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView)
    }

    // 현위치 가져 오는 버튼 상단쪽으로 이동하고 싶어서 직접 xml에 작성후 초기화 (기본으로 나오는값은 false)
    private val currentLocationButton: LocationButtonView by lazy {
        findViewById(R.id.currentLocationButton)
    }

    // 하단에 있는 바텀 시트에 텍스트뷰 값 (아이템이 몇개인지 표시)
    private val bottomSheetTitleTextView: TextView by lazy {
        findViewById(R.id.bottomSheetTitleTextView)
    }

    // 인텐트로 공유 하는 클릭 이벤트 구현
    private val viewPagerAdapter = HouseViewPagerAdapter(itemClicked = {
        val intent = Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "[지금 이 가격에 예약하세요!!] ${it.title} ${it.price} 사진보기 : ${it.imgUrl}"
                )
                type = "text/plain"
            }
        // 공유할 앱 선택 목록 띄우기 createChooser
        startActivity(Intent.createChooser(intent, null))

    })
    private val recyclerAdapter = HouseListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        // 맵뷰 naverMap 인스턴스 초기화
        mapView.getMapAsync(this)

        // 뷰페이저 초기화 , 리사이클러뷰 초기화
        viewPager.adapter = viewPagerAdapter
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 뷰페이저가 이동 되었을때 호출되는 함수
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // 현재 아이템값
                val selectedHouseModel = viewPagerAdapter.currentList[position]

                // 네이버 지도 이동
                // 이동 애니메이션 추가
                val cameraUpdate =
                    CameraUpdate.scrollTo(LatLng(selectedHouseModel.lat, selectedHouseModel.lng))
                        .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)
            }

        })
    }

    // 콜백 구현체
    override fun onMapReady(map: NaverMap) {
        naverMap = map

        // 줌설정
        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0

        // 화면 이동
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.497885, 127.027512))
        naverMap.moveCamera(cameraUpdate)

        //현 위치 가져 오기 버튼 활성화
        val uiSetting = naverMap.uiSettings
        // 기본 으로 나오는 현위치 버튼 안나오게
        uiSetting.isLocationButtonEnabled = false

        // 내가 구현한 버튼에 네이버 맵 연결
        currentLocationButton.map = naverMap

        // 현위치 가져오기 , 권한부여
        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

        // moky에서 데이터 받아오기
        getHouseListFromAPI()
    }

    private fun getHouseListFromAPI() {
        // 레트로핏 인스턴스 생성
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 레트로핏 초기화 사용
        retrofit.create(HouseService::class.java).also {
            it.getHouseList()
                .enqueue(object : Callback<HouseDto> {
                    override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                        if (response.isSuccessful.not()) {
                            // 실패 처리에 대한 구현
                            return
                        }


                        response.body()?.let { dto ->
                            // 마커 추가
                            updateMarker(dto.items)
                            viewPagerAdapter.submitList(dto.items)
                            recyclerAdapter.submitList(dto.items)

                            bottomSheetTitleTextView.text = "${dto.items.size}개의 숙소"
                        }
                    }

                    override fun onFailure(call: Call<HouseDto>, t: Throwable) {
                        // 실패 처리에 대한 구현
                    }


                })
        }
    }

    private fun updateMarker(houses: List<HouseModel>) {
       // 여러개의 아이템이 한번에 넘어오니까 forEach 반복문을 통해서 마커 생성
        houses.forEach { house ->
            val marker = Marker()
            // 위치 설정
            marker.position = LatLng(house.lat, house.lng)
            // 클릭 리스너 설정
            marker.onClickListener = this
            // 네이버 맵 연결
            marker.map = naverMap
            // 태그값에 서버로 받아온 id값 전달
            marker.tag = house.id
            // 아이콘 변경
            marker.icon = MarkerIcons.BLACK
            // 컬러값 변경
            marker.iconTintColor = Color.RED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // requestCode가 내가 설정한 LOCATION_PERMISSION_REQUEST_CODE 가 아닐경우 종료
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        // 성공적으로 현재값 가져 왔을때
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            // 활성화 되지 않았을 때
            if (!locationSource.isActivated) {
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }

    }

    // 맵뷰의 생명 주기 연결

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    // 메모리 부족할 때
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        // 리퀘스트 코드값 따로 상수로 빼기
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    // 맵 온클릭 리스너
    override fun onClick(overly: Overlay): Boolean {

        // 리스트에서 마커 선택된 tag값과 같은 id값 찾아서 해당 인덱스의 model값 selectModel에 저장
        // firstOrNull 매칭되는 첫번째 반환 하거나 찾지못하면 null값 반환
        val selectedModel = viewPagerAdapter.currentList.firstOrNull {
            it.id == overly.tag
        }

        // 뷰페이저 이동
        // null 일수도 있기 때문에 let ?연산자 사용
        selectedModel?.let {
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }

        return true
    }


}