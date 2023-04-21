package com.example.gps_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.gps_project.databinding.ActivityMapBinding
import com.example.gps_project.model.LocationLatLngEntity
import com.example.gps_project.model.SearchResultEntity
import com.example.gps_project.utility.RetrofitUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MapActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private var currentSelectMarker: Marker? = null

    private lateinit var searchResult: SearchResultEntity

    private lateinit var locationManager: LocationManager // 안드로이드 에서 위치정보 불러올 때 관리해주는 유틸 클래스

    private lateinit var myLocationListener: MyLocationListener // 나의 위치를 불러올 리스너

    // 두 지점 거리 차 구하기 위한 변수
    private var lat_S = 0.0
    private var lng_S = 0.0
    private var lat_C = 0.0
    private var lng_C = 0.0

    private var searchAddress = "null"

    companion object {
        const val SEARCH_RESULT_EXTRA_KEY: String = "SEARCH_RESULT_EXTRA_KEY"
        const val CAMERA_ZOOM_LEVEL = 17f
        const val PERMISSION_REQUEST_CODE = 2021
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        job = Job()

        if (::searchResult.isInitialized.not()) {
            intent?.let {
                searchResult = it.getParcelableExtra<SearchResultEntity>(SEARCH_RESULT_EXTRA_KEY)
                    ?: throw Exception("데이터가 존재하지 않습니다.")
                setupGoogleMap()
            }
        }

        bindViews()
    }

    override fun onBackPressed() {
        val intent = Intent()
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun bindViews() = with(binding) {
        // 현재 위치 버튼 리스너
        currentLocationButton.setOnClickListener {
            binding.progressCircular.isVisible = true
            getMyLocation()
        }
    }


    private fun setupGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.mapFragment.id) as SupportMapFragment
        mapFragment.getMapAsync(this) // callback 구현 (onMapReady)

        // 마커 데이터 보여주기

    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map
        currentSelectMarker = setupMarker(searchResult)

        currentSelectMarker?.showInfoWindow()
    }

    private fun setupMarker(searchResult: SearchResultEntity): Marker {

        // 구글맵 전용 위도/경도 객체
        val positionLatLng = LatLng(
            searchResult.locationLatLng.latitude.toDouble(),
            searchResult.locationLatLng.longitude.toDouble()
        )
        if (lat_S == 0.0 && lng_S == 0.0) {
            lat_S = searchResult.locationLatLng.latitude.toDouble()
            lng_S = searchResult.locationLatLng.longitude.toDouble()
        }

        // 구글맵 마커 객체 설정
        val markerOptions = MarkerOptions().apply {
            position(positionLatLng)
            title(searchResult.name)
            snippet(searchResult.fullAddress)
        }
        if (searchAddress == "null") {
            searchAddress = searchResult.fullAddress
        }

        // 카메라 줌 설정
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(positionLatLng, CAMERA_ZOOM_LEVEL))

        return map.addMarker(markerOptions)
    }

    private fun getMyLocation() {
        // 위치 매니저 초기화
        if (::locationManager.isInitialized.not()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        // GPS 이용 가능한지
        val isGpsEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // 권한 얻기
        if (isGpsEnable) {
            when {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) -> {
                    showPermissionContextPop()
                }

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED -> {
                    makeRequestAsync()
                }

                else -> {
                    setMyLocationListener()
                }
            }
        }
    }

    private fun showPermissionContextPop() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("내 위치를 불러오기위해 권한이 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                makeRequestAsync()
            }
            .create()
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocationListener() {
        val minTime = 3000L // 현재 위치를 불러오는데 기다릴 최소 시간
        val minDistance = 100f // 최소 거리 허용

        // 로케이션 리스너 초기화
        if (::myLocationListener.isInitialized.not()) {
            myLocationListener = MyLocationListener()
        }

        // 현재 위치 업데이트 요청
        with(locationManager) {
            requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
            requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
        }
    }

    private fun onCurrentLocationChanged(locationLatLngEntity: LocationLatLngEntity) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    locationLatLngEntity.latitude.toDouble(),
                    locationLatLngEntity.longitude.toDouble()
                ), CAMERA_ZOOM_LEVEL
            )
        )
        lat_C = locationLatLngEntity.latitude.toDouble()
        lng_C = locationLatLngEntity.longitude.toDouble()

        loadReverseGeoInformation(locationLatLngEntity)
        removeLocationListener() // 위치 불러온 경우 더이상 리스너가 필요 없으므로 제거
    }

    private fun loadReverseGeoInformation(locationLatLngEntity: LocationLatLngEntity) {
        // 코루틴 사용
        launch(coroutineContext) {
            try {
                binding.progressCircular.isVisible = true

                // IO 스레드에서 위치 정보를 받아옴
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.apiService.getReverseGeoCode(
                        lat = locationLatLngEntity.latitude.toDouble(),
                        lon = locationLatLngEntity.longitude.toDouble()
                    )
                    if (response.isSuccessful) {
                        val body = response.body()

                        // 응답 성공한 경우 UI 스레드에서 처리
                        withContext(Dispatchers.Main) {
                            Log.e("list", body.toString())
                            body?.let {
                                currentSelectMarker = setupMarker(
                                    SearchResultEntity(
                                        fullAddress = it.addressInfo.fullAddress ?: "주소 정보 없음",
                                        name = "내 위치",
                                        locationLatLng = locationLatLngEntity
                                    )
                                )
                                // 마커 보여주기
                                currentSelectMarker?.showInfoWindow()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapActivity, "검색하는 과정에서 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressCircular.isVisible = false
            }
        }
    }

    private fun removeLocationListener() {
        if (::locationManager.isInitialized && ::myLocationListener.isInitialized) {
            locationManager.removeUpdates(myLocationListener) // myLocationListener 를 업데이트 대상에서 지워줌
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setMyLocationListener()
                } else {
                    Toast.makeText(this, "권한을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                    binding.progressCircular.isVisible = false
                }
            }
        }
    }

    private fun makeRequestAsync() {
        // 퍼미션 요청 작업. 아래 작업은 비동기로 이루어짐
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    inner class MyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            // 현재 위치 콜백
            val locationLatLngEntity = LocationLatLngEntity(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )

            onCurrentLocationChanged(locationLatLngEntity)

            var btn: FloatingActionButton = findViewById(R.id.calc_button)
            btn.setOnClickListener {
                getDistance()
            }
        }
    }

    private fun getDistance() {

        if (lat_S != 0.0 && lng_S != 0.0 && lat_C != 0.0 && lng_C != 0.0) {
            val earthR = 6371000.0
            val rad = Math.PI / 180
            val radLat1 = rad * lat_S
            val radLat2 = rad * lat_C
            val radDist = rad * (lng_S - lng_C)

            var distance = Math.sin(radLat1) * Math.sin(radLat2)
            distance = distance + Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radDist)
            val ret = earthR * Math.acos(distance)

            val result = ret.roundToLong() // 미터 단위

            if (result <= 100) {
                Toast.makeText(this, "출석 완료! (" + result + "m)", Toast.LENGTH_SHORT).show()
                val intent = Intent()
                intent.putExtra("fullAddress", searchAddress)
                intent.putExtra("lat_S", lat_S)
                intent.putExtra("lng_S", lng_S)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "거리차: " + result + "m\n인증 실패, 거리가 100m 이상입니다",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent()
                setResult(2, intent)
                finish()
            }
        }

    }
}