package com.example.navermapgcs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;

    private Spinner modeSelector;

    private Button startVideoStream;
    private Button stopVideoStream;

    private Button startVideoStreamUsingObserver;
    private Button stopVideoStreamUsingObserver;

    private MediaCodecManager mediaCodecManager;

    private TextureView videoView;

    private String videoTag = "testvideotag";

    private Marker markerGPS = new Marker();

    Handler mainHandler;

    NaverMap myMap;
    //private Context context;
    boolean blayer, sw = false;
    Button btn1, btn2;
    double high = 3.0;
    final Marker guideMarker = new Marker();
    List<LatLng> coords = new ArrayList<>();
    LatLng GuideLat;

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner)findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        btn1 = findViewById(R.id.btnMapType);
        btn2 = findViewById(R.id.Layer);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                blayer = !blayer;
                showLayer();
            }
        });
    }

    void show()
    {
        final List<String> ListItems = new ArrayList<>();
        ListItems.add("Basic");
        ListItems.add("Navi");
        ListItems.add("Sattlelite");
        ListItems.add("Hybrid");
        ListItems.add("Terrain");
        final CharSequence[] items =  ListItems.toArray(new String[ ListItems.size()]);

        final List SelectedItems  = new ArrayList();
        int defaultItem = 0;
        SelectedItems.add(defaultItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map Type");
        builder.setSingleChoiceItems(items, defaultItem,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems.clear();
                        SelectedItems.add(which);
                    }
                });
        builder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String msg="";

                        if (!SelectedItems.isEmpty()) {
                            int index = (int) SelectedItems.get(0);
                            msg = ListItems.get(index);
                            if(index == 0){
                                myMap.setMapType(NaverMap.MapType.Basic);
                                btn1.setText("Baasic");
                            }
                            if(index == 1) {
                                myMap.setMapType(NaverMap.MapType.Navi);
                                btn1.setText("Navi");
                            }
                            if(index == 2) {
                                myMap.setMapType(NaverMap.MapType.Satellite);
                                btn1.setText("Satellite");
                            }
                            if(index == 3) {
                                myMap.setMapType(NaverMap.MapType.Hybrid);
                                btn1.setText("Hybrid");
                            }
                            if(index == 4) {
                                myMap.setMapType(NaverMap.MapType.Terrain);
                                btn1.setText("Terrain");
                            }
                        }
                        Toast.makeText(getApplicationContext(),
                                "Map Type Selected.\n"+ msg , Toast.LENGTH_LONG)
                                .show();
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    void showLayer(){
        if(blayer == true){
            myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
            btn2.setText("LAYER ON");
            Toast.makeText(getApplicationContext(),
                    "Map Layer Selected.\n"+ "지적편집도 ON" , Toast.LENGTH_LONG).show();
        }
        else {
            myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
            btn2.setText("LAYER OFF");
            Toast.makeText(getApplicationContext(),
                    "Map Layer Selected.\n" + "지적편집도 OFF", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                myMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(final NaverMap naverMap) {
        this.myMap = naverMap;
        naverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                guideMarker.setPosition(latLng);
                guideMarker.setMap(naverMap);
                guideMode(latLng);
                GuideLat = latLng;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.GPS_COUNT:
                updateGPS_cnt();
                break;

            case AttributeEvent.GPS_POSITION:
                updateGPS_pot();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(high, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    public void onAltitudeTap(View view) {
        final Button altitudeButton = (Button) findViewById(R.id.setAltitude);
        final Button altitudePlusButton = (Button) findViewById(R.id.setAltitudePlus);
        final Button altitudeMinusButton = (Button) findViewById(R.id.setAltitudeMinus);
        altitudeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                sw = !sw;
                if(sw == false) {
                    altitudePlusButton.setVisibility(View.INVISIBLE);
                    altitudeMinusButton.setVisibility(View.INVISIBLE);
                }
                if(sw == true){
                    altitudePlusButton.setVisibility(View.VISIBLE);
                    altitudeMinusButton.setVisibility(View.VISIBLE);
                }
            }
        });
        altitudePlusButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                high += 0.5;
                Toast.makeText(getApplicationContext(),
                        "고도 " + high + "m", Toast.LENGTH_LONG).show();
                altitudeButton.setText(String.valueOf(high + "m"));
            }
        });
        altitudeMinusButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                high -= 0.5;
                Toast.makeText(getApplicationContext(),
                        "고도 " + high + "m", Toast.LENGTH_LONG).show();
                altitudeButton.setText(String.valueOf(high + "m"));
            }
        });
        altitudeButton.setText(String.valueOf(high + "m"));
    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("고도 " +"%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("속도 " + "%d", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateBattery() {
        TextView BatteryTextView = (TextView) findViewById(R.id.batteryStateTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        BatteryTextView.setText(String.format("전압 " + "%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    protected void updateYaw(){
        TextView YawTextView = (TextView) findViewById(R.id.YawValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(droneYaw.getYaw() < 0){
            YawTextView.setText(String.format("YAW " + "%3.1f", (droneYaw.getYaw())+360) + "deg");
            markerGPS.setAngle((float) droneYaw.getYaw() +360);
        }
        else {
            YawTextView.setText(String.format("YAW " + "%3.1f", droneYaw.getYaw()) + "deg");
            markerGPS.setAngle((float) droneYaw.getYaw());
        }
    }

    protected void updateGPS_cnt(){
        TextView GPSTextView = (TextView) findViewById(R.id.GPSValueTextView);
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        GPSTextView.setText(String.format("위성 " + "%d", droneGPS.getSatellitesCount()));

    }

    protected void updateGPS_pot(){
        Gps drone_poGPS = this.drone.getAttribute(AttributeType.GPS);
        LocationOverlay locationOverlay = myMap.getLocationOverlay();
        LatLong dronePostionLatLong;
        LatLng dronePosition;
        PolylineOverlay polyline = new PolylineOverlay();
        State GuideVehicleState = this.drone.getAttribute(AttributeType.STATE);

        try {
            dronePostionLatLong = drone_poGPS.getPosition();
            dronePosition = new LatLng(dronePostionLatLong.getLatitude(), dronePostionLatLong.getLongitude());
            Collections.addAll(coords, dronePosition);
            polyline.setCoords(coords);
            polyline.setMap(myMap);
        } catch (Exception e) {
            Log.d("myLog","위치를 못 가지고 오는 에러 : "+e.getMessage());
            dronePosition = new LatLng(95, 37);
        }
        markerGPS.setIcon(OverlayImage.fromResource(R.drawable.marker_icon));
        markerGPS.setPosition(dronePosition);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(dronePosition);
        myMap.moveCamera(cameraUpdate);
        locationOverlay.setPosition(dronePosition);
        markerGPS.setMap(myMap);

        if (GuideVehicleState.isFlying()) {
            if((GuideVehicleState.getVehicleMode() == VehicleMode.COPTER_GUIDED)){
                if(CheckGoal(drone, GuideLat) == true){
                    guideMarker.setMap(null);
                    Toast.makeText(getApplicationContext(), "체크 포인트에 도착했습니다. 가이드 모드를 종료합니다", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    protected void guideMode(final LatLng guideLatLng){
        final AlertDialog.Builder alt_bld = new AlertDialog.Builder(MainActivity.this);
        State guideVehicleState = this.drone.getAttribute(AttributeType.STATE);

        alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Action for 'Yes' Button
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        ControlApi.getApi(drone).goTo(new LatLong(guideLatLng.latitude, guideLatLng.longitude), true, null);
                    }
                    @Override
                    public void onError(int i) {
                    }
                    @Override
                    public void onTimeout() {
                    }
                });
            }
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                guideMarker.setMap(null);
            }
        });

        AlertDialog alert = alt_bld.create();
        if (guideVehicleState.isFlying()){
            alert.show();
        }
    }

    public boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        Toast.makeText(this, "가이드모드 고도 : " + guidedState.getCoordinate().getAltitude(), Toast.LENGTH_LONG).show();
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }


    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }
}