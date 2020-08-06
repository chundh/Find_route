package com.example.find_route;

/**
 * Created by 천도희 on 2017-09-05.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class TMapActivity extends Activity implements TMapGpsManager.onLocationChangedCallback, RadioGroup.OnCheckedChangeListener {

    TMapView mapView;

    LocationManager mLM;
    String mProvider = LocationManager.NETWORK_PROVIDER;
    TextView textView,mn,ma; //mn=마커이름, ma = 마커주소
    EditText keywordView;
    ListView listView;
    ArrayAdapter<POI> mAdapter;
    Button myloc, btn_search, btn_route, btn_can, btn_ok, btn_start, btn_end;
    TMapPoint start, end, s, e;
    RadioGroup typeView, route_radio;
    RadioButton rad_ped, rad_car;
    TMapGpsManager tMapGpsManager;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    FrameLayout mar;
    double lat;
    double lon;
    String mname,mad;

    Context mContext;

    //-------------------------------------------------------------------------------DB
    public static final String ROOT_DIR = "/data/data/com.example.find_route/";
    public static String DATABASE_NAME = "lightDB.db";
    //-------------------------------------------------------------------------------DB
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tmap);
        //gps 관한권한 허가요청. 안드로이드 6.0이상에서 문제가 발생하는 경우가 있음.
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);}

        typeView = (RadioGroup) findViewById(R.id.group_type);
        keywordView = (EditText) findViewById(R.id.edit_keyword);
        listView = (ListView) findViewById(R.id.listView);
        mAdapter = new ArrayAdapter<POI>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(mAdapter);
        listView.setVisibility(View.INVISIBLE);
        myloc = (Button) findViewById(R.id.showloc);
        tMapGpsManager = new TMapGpsManager(TMapActivity.this);
        tMapGpsManager.setMinDistance(1);  //최소 인식거리 설정
        tMapGpsManager.setMinTime(1000);   //최소 인식시간 설정
        btn_can = (Button) findViewById(R.id.btn_can);
        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_can.setVisibility(View.INVISIBLE);
        btn_ok.setVisibility(View.INVISIBLE);
        btn_start = (Button)findViewById(R.id.start);
        btn_end = (Button)findViewById(R.id.end);
        route_radio = (RadioGroup) findViewById(R.id.route_radio);
        route_radio.setVisibility(View.INVISIBLE);
        rad_car = (RadioButton) findViewById(R.id.car);
        rad_ped = (RadioButton) findViewById(R.id.ped);
        textView = (TextView) findViewById(R.id.textView);
        textView.setVisibility(View.INVISIBLE);
        mn = (TextView) findViewById(R.id.mname);
        ma = (TextView) findViewById(R.id.mad);
        mar = (FrameLayout) findViewById(R.id.marker);
        mar.setVisibility(View.INVISIBLE);
        mLM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mapView = (TMapView) findViewById(R.id.map_view);
        //--------------------------------------------------------------DB

        mContext = TMapActivity.this;

        initialize(mContext);
        SQLiteDatabase db = null;

        db.openDatabase(ROOT_DIR+"databases/"+DATABASE_NAME,null,SQLiteDatabase.OPEN_READWRITE);
        final DBHelper dbHelper = new DBHelper(mContext,DATABASE_NAME,null,1);
        String str = dbHelper.getResult(); //이게 다 받아오는 메소드

        //--------------------------------------------------------------DB
        mapView.setOnApiKeyListener(new TMapView.OnApiKeyListenerCallback() {
            @Override
            public void SKPMapApikeySucceed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupMap();
                    }
                });
            }

            @Override
            public void SKPMapApikeyFailed(String s) {

            }
        });
        mapView.setSKPMapApiKey("3aa21a7b-cc1b-3bef-b97e-5fa444bb8091");
        mapView.setLanguage(TMapView.LANGUAGE_KOREAN);

        Button btn = (Button) findViewById(R.id.btn_add_marker);
        btn.setOnClickListener(new View.OnClickListener() {  //현재 지도의 중앙에 마커 등록.
            @Override
            public void onClick(View view) {
                TMapPoint point = mapView.getCenterPoint();
                addMarker(point.getLatitude(), point.getLongitude(), "내가 추가한 마커");
            }
        });

        myloc.setOnClickListener(new View.OnClickListener() {  //myloc버튼을 누르면 자신의 현재위치로 지도를 이동
            @Override
            public void onClick(View v) {
                tMapGpsManager.setProvider(tMapGpsManager.GPS_PROVIDER);
                tMapGpsManager.setProvider(tMapGpsManager.NETWORK_PROVIDER);

                tMapGpsManager.OpenGps();
                mapView.setCenterPoint(lat,lon);
                mapView.setTrackingMode(true);
                mapView.setSightVisible(true);
            }
        });

        btn_search = (Button) findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.setVisibility(View.VISIBLE);
                searchPOI();
            }
        });  //SEARCH 버튼을 누르면 아이템 리스트에 검색 목록이 나옴

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) { //아이템리스트에 아이템을 누르면 해당 좌표로 지도 이동.
                listView.setVisibility(View.INVISIBLE);
                final POI poi = (POI) listView.getItemAtPosition(position);
                moveMap(poi.item.getPOIPoint().getLatitude(), poi.item.getPOIPoint().getLongitude());
                mname = poi.item.getPOIName();
                mad = poi.item.getPOIAddress();
                mn.setText(mname);
                ma.setText(mad);
                mar.setVisibility(View.VISIBLE);
                btn_start.setOnClickListener(new View.OnClickListener() { //출발지로 버튼 눌렀을때
                    @Override
                    public void onClick(View v) {  //출발지 위도, 경도 저장, marker 안보이게
                        s = poi.item.getPOIPoint();
                        mar.setVisibility(View.INVISIBLE);
                    }
                });
                btn_end.setOnClickListener(new View.OnClickListener() { // 도착지로 버튼 눌렀을때
                    @Override
                    public void onClick(View v) {  //도착지 위도, 경도 저장, marker 안보이게
                        e = poi.item.getPOIPoint();
                        mar.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        /*
        btn_route = (Button) findViewById(R.id.btn_route);
        btn_route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //탐색
                if (s != null && e != null) {
                    searchRoute(s, e);
                } else {
                    Toast.makeText(TMapActivity.this, "start or end is null", Toast.LENGTH_SHORT).show();
                }
            }
        });
        */
        Toast.makeText(TMapActivity.this, dbHelper.getResult().toString(), Toast.LENGTH_LONG).show();
    }

    private void searchRoute(TMapPoint start, TMapPoint end) {
        s = start;
        e = end;
        route_radio.setVisibility(View.VISIBLE);
        btn_can.setVisibility(View.VISIBLE);
        btn_ok.setVisibility(View.VISIBLE);
        route_radio.setOnCheckedChangeListener(this);
    }   //경로탐색



    private void searchPOI() {  //검색값에 관한 정보를 가져오는 함수
        TMapData data = new TMapData();
        String keyword = keywordView.getText().toString();
        if (!TextUtils.isEmpty(keyword)) {
            data.findAllPOI(keyword, new TMapData.FindAllPOIListenerCallback() {
                @Override
                public void onFindAllPOI(final ArrayList<TMapPOIItem> arrayList) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mapView.removeAllMarkerItem();
                            mAdapter.clear();

                            for (TMapPOIItem poi : arrayList) {
                                addMarker(poi);
                                mAdapter.add(new POI(poi));
                            }

                            if (arrayList.size() > 0) {
                                TMapPOIItem poi = arrayList.get(0);
                                moveMap(poi.getPOIPoint().getLatitude(), poi.getPOIPoint().getLongitude());
                            }
                        }
                    });
                }
            });
        }
    }

    public void addMarker(TMapPOIItem poi) {  //검색값의 위치에 대한 마커를 추가
        TMapMarkerItem item = new TMapMarkerItem();
        item.setTMapPoint(poi.getPOIPoint());
        Bitmap icon = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_input_add)).getBitmap();
        item.setIcon(icon);
        item.setPosition(0.5f, 1);
        item.setCalloutTitle(poi.getPOIName());
        item.setCalloutSubTitle(poi.getPOIContent());
        Bitmap left = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)).getBitmap();
        item.setCalloutLeftImage(left);
        Bitmap right = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_input_get)).getBitmap();
        item.setCalloutRightButtonImage(right);
        item.setCanShowCallout(true);
        mapView.addMarkerItem(poi.getPOIID(), item);
    }

    private void addMarker(double lat, double lng, String title) {   //현재 내가 보고있는 위치를 마커로 지정
        TMapMarkerItem item = new TMapMarkerItem();
        TMapPoint point = new TMapPoint(lat, lng);
        item.setTMapPoint(point);
        Bitmap icon = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_input_add)).getBitmap();
        item.setIcon(icon);
        item.setPosition(0.5f, 1);
        item.setCalloutTitle(title);
        item.setCalloutSubTitle("sub " + title);
        Bitmap left = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_alert)).getBitmap();
        item.setCalloutLeftImage(left);
        Bitmap right = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_input_get)).getBitmap();
        item.setCalloutRightButtonImage(right);
        item.setCanShowCallout(true);
        mapView.addMarkerItem("m" + id, item);
        id++;
    }

    int id = 0;

    boolean isInitialized = false;

    private void setupMap() {
        isInitialized = true;
        mapView.setMapType(TMapView.MAPTYPE_STANDARD);
        if (cacheLocation != null) {
            moveMap(cacheLocation.getLatitude(), cacheLocation.getLongitude());
            setMyLocation(cacheLocation.getLatitude(), cacheLocation.getLongitude());
        }
        mapView.setOnCalloutRightButtonClickListener(new TMapView.OnCalloutRightButtonClickCallback() {
            @Override
            public void onCalloutRightButton(final TMapMarkerItem tMapMarkerItem) {
                /*
                String message = null;
                switch (typeView.getCheckedRadioButtonId()) {
                    case R.id.radio_start:
                        start = tMapMarkerItem.getTMapPoint();
                        message = "start";
                        break;
                    case R.id.radio_end:
                        end = tMapMarkerItem.getTMapPoint();
                        message = "end";
                        break;
                }
                Toast.makeText(TMapActivity.this, message + " setting", Toast.LENGTH_SHORT).show();
                */
                mar.setVisibility(View.VISIBLE);
                btn_start.setOnClickListener(new View.OnClickListener() { //출발지로 버튼 눌렀을때
                    @Override
                    public void onClick(View v) {  //출발지 위도, 경도 저장, marker 안보이게
                        s = tMapMarkerItem.getTMapPoint();
                        mar.setVisibility(View.INVISIBLE);
                        if(e!=null){
                            searchRoute(s,e);
                        }
                    }
                });
                btn_end.setOnClickListener(new View.OnClickListener() { // 도착지로 버튼 눌렀을때
                    @Override
                    public void onClick(View v) {  //도착지 위도, 경도 저장, marker 안보이게
                        e = tMapMarkerItem.getTMapPoint();
                        mar.setVisibility(View.INVISIBLE);
                        if(s!=null){
                            searchRoute(s,e);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = mLM.getLastKnownLocation(mProvider);
        if (location != null) {
            mListener.onLocationChanged(location);
        }
        mLM.requestSingleUpdate(mProvider, mListener, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLM.removeUpdates(mListener);
    }

    Location cacheLocation = null;

    private void moveMap(double lat, double lng) {
        mapView.setCenterPoint(lng, lat);
    }

    private void setMyLocation(double lat, double lng) {
        Bitmap icon = ((BitmapDrawable) ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_map)).getBitmap();
        mapView.setIcon(icon);
        mapView.setLocationPoint(lng, lat);
        mapView.setIconVisibility(true);
    }

    LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (isInitialized) {
                moveMap(location.getLatitude(), location.getLongitude());
                setMyLocation(location.getLatitude(), location.getLongitude());
            } else {
                cacheLocation = location;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    public void onLocationChange(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        TMapData data = new TMapData(); //보행자 경로
        TMapData data1 = new TMapData();  //자동차 경로
        if(checkedId==R.id.ped) {
            data.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, s, e, new TMapData.FindPathDataListenerCallback() {
                @Override
                public void onFindPathData(final TMapPolyLine path) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            path.setLineWidth(5);
                            path.setLineColor(Color.RED);
                            mapView.addTMapPath(path);
                            Bitmap s = ((BitmapDrawable) ContextCompat.getDrawable(TMapActivity.this, android.R.drawable.ic_input_delete)).getBitmap();
                            Bitmap e = ((BitmapDrawable) ContextCompat.getDrawable(TMapActivity.this, android.R.drawable.ic_input_get)).getBitmap();
                            mapView.setTMapPathIcon(s, e);
                        }
                    });
                    btn_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { //경로탐색시작
                            route_radio.setVisibility(View.INVISIBLE);
                            btn_ok.setVisibility(View.INVISIBLE);
                            btn_can.setVisibility(View.INVISIBLE);
                            textView.setVisibility(View.VISIBLE);
                            navigate();
                        }
                    });
                    btn_can.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { //경로탐색 취소
                            route_radio.setVisibility(View.INVISIBLE);
                            btn_ok.setVisibility(View.INVISIBLE);
                            btn_can.setVisibility(View.INVISIBLE);
                            mapView.removeTMapPath();
                            s = null;
                            e = null;
                        }
                    });
                }
            });
        }
        else if(checkedId==R.id.car) {
            data1.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, s, e, new TMapData.FindPathDataListenerCallback() {
                @Override
                public void onFindPathData(final TMapPolyLine path) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            path.setLineWidth(5);
                            path.setLineColor(Color.GREEN);
                            mapView.addTMapPath(path);
                            Bitmap s = ((BitmapDrawable) ContextCompat.getDrawable(TMapActivity.this, android.R.drawable.ic_input_delete)).getBitmap();
                            Bitmap e = ((BitmapDrawable) ContextCompat.getDrawable(TMapActivity.this, android.R.drawable.ic_input_get)).getBitmap();
                            mapView.setTMapPathIcon(s, e);
                        }
                    });
                    btn_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            route_radio.setVisibility(View.INVISIBLE);
                            btn_ok.setVisibility(View.INVISIBLE);
                            btn_can.setVisibility(View.INVISIBLE);
                            textView.setVisibility(View.VISIBLE);
                            navigate();
                        }
                    });
                    btn_can.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            route_radio.setVisibility(View.INVISIBLE);
                            btn_ok.setVisibility(View.INVISIBLE);
                            btn_can.setVisibility(View.INVISIBLE);
                            mapView.removeTMapPath();
                            s = null;
                            e = null;
                        }
                    });
                }
            });
        }
    }
    void navigate() {
        tMapGpsManager.setProvider(tMapGpsManager.GPS_PROVIDER);
        tMapGpsManager.setProvider(tMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();
        TimerTask task = new TimerTask(){
            public void run() {
                try {
                    mapView.setCenterPoint(lat, lon);
                    mapView.setTrackingMode(true);
                    mapView.setSightVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Timer mTimer = new Timer();
        mTimer.schedule(task, 0, 500);  //바로 task실행하고 0.5초주기로 task반복
        mTimer.cancel(); //반복종료
    }
    public static void initialize(Context ctx)
    {
        Toast.makeText(ctx,"시작",Toast.LENGTH_LONG);
        File folder = new File(ROOT_DIR + "databases");
        folder.mkdirs();
        File outfile = new File(ROOT_DIR + "databases/" + DATABASE_NAME);
        if (outfile.length() <= 0) {
            AssetManager assetManager = ctx.getResources().getAssets();
            try {
                InputStream is = assetManager.open(DATABASE_NAME, AssetManager.ACCESS_BUFFER);
                long filesize = is.available();
                byte [] tempdata = new byte[(int)filesize];
                is.read(tempdata);
                is.close();
                outfile.createNewFile();
                FileOutputStream fo = new FileOutputStream(outfile);
                fo.write(tempdata);
                fo.close();

                Toast.makeText(ctx,"성공",Toast.LENGTH_LONG);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
        }
    }



}
