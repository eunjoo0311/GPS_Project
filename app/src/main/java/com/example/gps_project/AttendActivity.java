package com.example.gps_project;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gps_project.Fragment.HomeFragment;
import com.example.gps_project.model.Attendance;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.naishadhparmar.zcustomcalendar.CustomCalendar;
import org.naishadhparmar.zcustomcalendar.OnDateSelectedListener;
import org.naishadhparmar.zcustomcalendar.OnNavigationButtonClickedListener;
import org.naishadhparmar.zcustomcalendar.Property;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AttendActivity extends AppCompatActivity implements OnNavigationButtonClickedListener, OnDateSelectedListener, EventListener<QuerySnapshot> {

    private static String ATTENDANCE_PATH;

    private FirebaseFirestore db;
    private SimpleDateFormat sdf;
    private CustomCalendar customCalendar;
    private View progressView;

    private Calendar selectedDate = null;
    private ListenerRegistration listenerRegistration;

    public static final int REQUEST_CODE_GPS = 101; // startActivityForResult

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attend);

        auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();
        String userid = firebaseUser.getUid();
        ATTENDANCE_PATH = userid;

        db = FirebaseFirestore.getInstance();
        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);

        customCalendar = (CustomCalendar) findViewById(R.id.custom_calendar);
        progressView = findViewById(R.id.progress_view);

        initCalendar();

        findViewById(R.id.attend_button).setOnClickListener(v -> attend());
        findViewById(R.id.explain_button).setOnClickListener(v -> explain());
    }

    /**
     * Initialize calendar
     */
    private void initCalendar() {
        //region Set property
        HashMap<Object, Property> mapDescToProp = new HashMap<>();

        Property propDefault = new Property();
        propDefault.layoutResource = R.layout.default_vi;
        propDefault.dateTextViewResource = R.id.text1;
        mapDescToProp.put("default", propDefault);

        Property propCheck = new Property();
        propCheck.layoutResource = R.layout.current;
        propCheck.dateTextViewResource = R.id.text1;
        mapDescToProp.put("check", propCheck);

        customCalendar.setMapDescToProp(mapDescToProp);
        //endregion

        customCalendar.setOnDateSelectedListener(this);
        customCalendar.setOnNavigationButtonClickedListener(CustomCalendar.PREVIOUS, this);
        customCalendar.setOnNavigationButtonClickedListener(CustomCalendar.NEXT, this);

        // onDateSelected 의 매개변수 desc 때문에, 아래 코드를 반드시 호출해야 함.
        Calendar month = Calendar.getInstance();
        setMonth(month);
        customCalendar.setDate(month, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * move to ExplainActivity
     */
    private void explain() {
        startActivity(new Intent(AttendActivity.this, ExplainActivity.class));
    }

    /**
     * Add firestore data
     */
    private void attend() {
        Date today = new Date();
        String id = sdf.format(today);

        DocumentReference docRef = db.collection(ATTENDANCE_PATH).document(id);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Toast.makeText(getApplicationContext(), "오늘 이미 출석하셨습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "출석 인증 조건: 검색한 위치가 현재 위치의 100m 이내", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(getApplicationContext(), GpsActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_GPS);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        assert data != null;
        if (resultCode == RESULT_OK) {
            // 출석 체크 메소드
            Date today = new Date();
            String id = sdf.format(today);

            Attendance attendance = new Attendance();
            attendance.setTimestamp(today);

            db.collection(ATTENDANCE_PATH).document(id).set(attendance);

            // 주소 정보 저장 메소드
            String fullAddress = data.getStringExtra("fullAddress");
            Double lat_S = data.getDoubleExtra("lat_S", 0.0);
            Double lng_S = data.getDoubleExtra("lng_S", 0.0);

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            String userid = firebaseUser.getUid();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Places").child(userid).child(fullAddress);

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("address", fullAddress);
            hashMap.put("date", id);
            hashMap.put("lat", lat_S);
            hashMap.put("lng", lng_S);

            reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(getApplicationContext(), "장소 DB 저장 완료", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "인증을 취소하였습니다", Toast.LENGTH_SHORT).show();
        }

        // TODO 액티비티 화면 재갱신 시키는 코드
        Intent intent = getIntent();
        finish(); // 현재 액티비티 종료 실시
        overridePendingTransition(0, 0); // 인텐트 애니메이션 없애기
        startActivity(intent); // 현재 액티비티 재실행 실시
        overridePendingTransition(0, 0); // 인텐트 애니메이션 없애기
    }

    /**
     * Add firestore snapshot listener
     *
     * @param date Selected date
     */
    private void addAttendanceSnapshotListener(Calendar date) {
        // 선택된 날짜와 매개변수 date 의 날짜가 동일하면 함수 종료
        if (selectedDate != null &&
                (selectedDate.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                        selectedDate.get(Calendar.MONTH) == date.get(Calendar.MONTH))
        ) {
            return;
        }

        progressView.setVisibility(View.VISIBLE);

        removeAttendanceSnapshotListener();

        selectedDate = Calendar.getInstance();
        selectedDate.setTime(date.getTime());

        Date from;  // Inclusive
        Date to;    // Exclusive

        Calendar clone = Calendar.getInstance();
        clone.setTime(date.getTime());
        setMonth(clone);

        from = clone.getTime();

        clone.add(Calendar.MONTH, 1);

        to = clone.getTime();

        // 선택된 달의 1일 자정부터, 다음달 1일 자정 전까지의 데이터를 가져온다.
        listenerRegistration = db.collection(ATTENDANCE_PATH)
                .whereGreaterThanOrEqualTo("timestamp", from)
                .whereLessThan("timestamp", to)
                .addSnapshotListener(this);
    }

    /**
     * Remove firestore snapshot listener
     */
    private void removeAttendanceSnapshotListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    /**
     * Calendar 의 날짜를 해당 달의 1일 자정으로 설정하는 함수
     *
     * @param calendar
     */
    private void setMonth(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        addAttendanceSnapshotListener(customCalendar.getSelectedDate());
    }

    @Override
    protected void onPause() {
        super.onPause();

        removeAttendanceSnapshotListener();
    }

    @Override
    public void onDateSelected(View view, Calendar selectedDate, Object desc) {
        Log.d("AttendActivity", "Selected date: " + sdf.format(selectedDate.getTime()));
    }

    @Override
    public Map<Integer, Object>[] onNavigationButtonClicked(int whichButton, Calendar newMonth) {
        Log.d("AttendActivity", "onNavigationButtonClicked: " + sdf.format(newMonth.getTime()));

        Map<Integer, Object>[] arr = new Map[2];
        arr[0] = new HashMap<>();
        arr[1] = new HashMap<>();

        addAttendanceSnapshotListener(newMonth);

        return arr;
    }

    /**
     * Firestore callback function
     *
     * @param value
     * @param error
     */
    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        if (value != null) {
            Map<Integer, Object> descriptions = new HashMap<>();
            Map<Integer, Object> tags = new HashMap<>();

            for (DocumentSnapshot snapshot : value.getDocuments()) {
                Attendance attendance = snapshot.toObject(Attendance.class);
                if (attendance == null) continue;

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(attendance.getTimestamp());

                descriptions.put(calendar.get(Calendar.DAY_OF_MONTH), "check");
                tags.put(calendar.get(Calendar.DAY_OF_MONTH), attendance);
            }

            customCalendar.setDate(customCalendar.getSelectedDate(), descriptions, tags);
        }

        progressView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    } // 뒤로가기 버튼 클릭했을 때 홈으로 이동하기
}