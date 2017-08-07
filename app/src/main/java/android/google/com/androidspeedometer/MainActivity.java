package android.google.com.androidspeedometer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


/**
 * Using location settings.
 * <p/>
 * Uses the {@link com.google.android.gms.location.SettingsApi} to ensure that the device's system
 * settings are properly configured for the app's location needs. When making a request to
 * Location services, the device's system settings may be in a state that prevents the app from
 * obtaining the location data that it needs. For example, GPS or Wi-Fi scanning may be switched
 * off. The {@code SettingsApi} makes it possible to determine if a device's system settings are
 * adequate for the location request, and to optionally invoke a dialog that allows the user to
 * enable the necessary settings.
 */
public class MainActivity extends AppCompatActivity
{

  private static final String TAG = MainActivity.class.getSimpleName();

  /**
   * Code used in requesting runtime permissions.
   */
  private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

  /**
   * Constant used in the location settings dialog.
   */
  private static final int REQUEST_CHECK_SETTINGS = 0x1;

  /**
   * The desired interval for location updates. Inexact. Updates may be more or less frequent.
   */
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 6000;

  /**
   * The fastest rate for active location updates. Exact. Updates will never be more frequent
   * than this value.
   */
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
    UPDATE_INTERVAL_IN_MILLISECONDS / 2;

  // Keys for storing activity state in the Bundle.
  private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
  private final static String KEY_LOCATION = "location";
  private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

  /**
   * Provides access to the Fused Location Provider API.
   */
  private FusedLocationProviderClient mFusedLocationClient;

  /**
   * Provides access to the Location Settings API.
   */
  private SettingsClient mSettingsClient;

  /**
   * Stores parameters for requests to the FusedLocationProviderApi.
   */
  private LocationRequest mLocationRequest;

  /**
   * Stores the types of location services the client is interested in using. Used for checking
   * settings to determine if the device has optimal location settings.
   */
  private LocationSettingsRequest mLocationSettingsRequest;

  /**
   * Callback for Location events.
   */
  private LocationCallback mLocationCallback;

  /**
   * Represents a geographical location.
   */
  private Location mCurrentLocation;

  // UI Widgets.
  private Button mStartStopButton;
  private TextView mSpeedTextView;
  private Chronometer simpleChronometer;

  /**
   * Tracks the status of the location updates request. Value changes when the user presses the
   * Start Updates and Stop Updates buttons.
   */
  private Boolean mRequestingLocationUpdates;


  /**
   * Time when the location was updated represented as a String.
   */
  private String mLastUpdateTime;

  //

  private String mStartAddress = "";
  private double mStartLatitude, mStartLongitude, mStopLatitude, mStopLongitude;

  private String mStartTime, mStopTime;

  private String mPreference, mTotalTime;

  private static final double mph = 2.23694;
  private static final double kph = 3.6;

  private double topSpeed = 0;

  private List<Double> speedArray = new ArrayList<>();


  String startAddess="", endAddress="";


  // http://maps.googleapis.com/maps/api/geocode/json?latlng=
  private static String url;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // custom font
    Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/myfont.ttf");

    // Locate the UI widgets.
    mSpeedTextView = (TextView) findViewById(R.id.speed_text);
    mSpeedTextView.setTypeface(custom_font);
    TextView mSpeedTypeTextView = (TextView) findViewById(R.id.speedTypeTextView);
    mSpeedTypeTextView.setTypeface(custom_font);
    simpleChronometer = (Chronometer) findViewById(R.id.simpleChronometer);
    simpleChronometer.setTypeface(custom_font);
    mStartStopButton = (Button) findViewById(R.id.start_updates_button);

    //Reading from SharedPreferences
    SharedPreferences pref = getApplicationContext().getSharedPreferences("speedPref", MODE_PRIVATE);
    mPreference = pref.getString("pref_type", null);

    Log.w("mPreference ", "" + mPreference);

    if (this.mPreference != null)
    {
      mSpeedTypeTextView.setText(mPreference);
    } else
    {
      this.mPreference = "miles";
    }

    // keep sceen on
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    // Add button listener
    this.mStartStopButton.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String buttonText = mStartStopButton.getText().toString();

       //   54.2936641, -7.0886426


        if (buttonText.equalsIgnoreCase("Start"))
        {
          startUpdatesButtonHandler(v);
        }
        else
        {
          stopUpdatesButtonHandler(v);
        }
      }
    });


    mRequestingLocationUpdates = false;
    mLastUpdateTime = "";

    // Update values using data stored in the Bundle.
    updateValuesFromBundle(savedInstanceState);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    // Kick off the process of building the LocationCallback, LocationRequest, and
    // LocationSettingsRequest objects.
    createLocationCallback();
    createLocationRequest();
    buildLocationSettingsRequest();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  // overflow menu
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    int id = item.getItemId();
    // Handle item selection - opens PrefActivity
    if (id == R.id.settings)
    {
      Intent intent = new Intent(this, PrefActivity.class);
      this.startActivity(intent);

      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * Updates fields based on data stored in the bundle.
   *
   * @param savedInstanceState The activity state saved in the Bundle.
   */
  private void updateValuesFromBundle(Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
      // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
      if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES))
      {
        mRequestingLocationUpdates = savedInstanceState.getBoolean(
          KEY_REQUESTING_LOCATION_UPDATES);
      }

      // Update the value of mCurrentLocation from the Bundle and update the UI to show the
      // correct latitude and longitude.
      if (savedInstanceState.keySet().contains(KEY_LOCATION))
      {
        // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
        // is not null.
        mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
      }

      // Update the value of mLastUpdateTime from the Bundle and update the UI.
      if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING))
      {
        mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING);
      }
      updateUI();
    }
  }

  /**
   * Sets up the location request. Android has two location request settings:
   * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
   * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
   * the AndroidManifest.xml.
   * <p/>
   * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
   * interval (5 seconds), the Fused Location Provider API returns location updates that are
   * accurate to within a few feet.
   * <p/>
   * These settings are appropriate for mapping applications that show real-time location
   * updates.
   */
  private void createLocationRequest()
  {
    mLocationRequest = new LocationRequest();

    // Sets the desired interval for active location updates. This interval is
    // inexact. You may not receive updates at all if no location sources are available, or
    // you may receive them slower than requested. You may also receive updates faster than
    // requested if other applications are requesting location at a faster interval.
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

    // Sets the fastest rate for active location updates. This interval is exact, and your
    // application will never receive updates faster than this value.
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  /**
   * Creates a callback for receiving location events.
   */
  private void createLocationCallback()
  {
    mLocationCallback = new LocationCallback()
    {
      @Override
      public void onLocationResult(LocationResult locationResult)
      {
        super.onLocationResult(locationResult);

        mCurrentLocation = locationResult.getLastLocation();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateLocationUI();
      }
    };
  }

  /**
   * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
   * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
   * if a device has the needed location settings.
   */
  private void buildLocationSettingsRequest()
  {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    switch (requestCode)
    {
      // Check for the integer request code originally supplied to startResolutionForResult().
      case REQUEST_CHECK_SETTINGS:
        switch (resultCode)
        {
          case Activity.RESULT_OK:
            Log.i(TAG, "User agreed to make required location settings changes.");
            // Nothing to do. startLocationupdates() gets called in onResume again.
            break;
          case Activity.RESULT_CANCELED:
            Log.i(TAG, "User chose not to make required location settings changes.");
            mRequestingLocationUpdates = false;
            updateUI();
            break;
        }
        break;
    }
  }

  /**
   * Handles the Start Updates button and requests start of location updates. Does nothing if
   * updates have already been requested.
   */
  public void startUpdatesButtonHandler(View view)
  {
    if (!mRequestingLocationUpdates)
    {
      mRequestingLocationUpdates = true;
      setButtonsEnabledState();
      startLocationUpdates();
    }
  }

  /**
   * Handles the Stop Updates button, and requests removal of location updates.
   */
  public void stopUpdatesButtonHandler(View view)
  {
    mStopTime = utils.getCurrentDateTime();
    mStopLatitude = mCurrentLocation.getLatitude();
    mStopLongitude = mCurrentLocation.getLongitude();

    url = "http://maps.googleapis.com/maps/api/geocode/json?latlng="+ mStopLatitude + "," + mStopLongitude;
    new GetApiData().execute();
  }

  /**
   * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
   * runtime permission has been granted.
   */
  private void startLocationUpdates()
  {
    // Begin by checking if the device has the necessary location settings.
    mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
      .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>()
      {
        @Override
        public void onSuccess(LocationSettingsResponse locationSettingsResponse)
        {
          Log.i(TAG, "All location settings are satisfied.");

          //noinspection MissingPermission
          mFusedLocationClient.requestLocationUpdates(mLocationRequest,
            mLocationCallback, Looper.myLooper());

          updateUI();
        }
      })
      .addOnFailureListener(this, new OnFailureListener()
      {
        @Override
        public void onFailure(@NonNull Exception e)
        {
          int statusCode = ((ApiException) e).getStatusCode();
          switch (statusCode)
          {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
              Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                "location settings ");
              try
              {
                // Show the dialog by calling startResolutionForResult(), and check the
                // result in onActivityResult().
                ResolvableApiException rae = (ResolvableApiException) e;
                rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
              } catch (IntentSender.SendIntentException sie)
              {
                Log.i(TAG, "PendingIntent unable to execute request.");
              }
              break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
              String errorMessage = "Location settings are inadequate, and cannot be " +
                "fixed here. Fix in Settings.";
              Log.e(TAG, errorMessage);
              Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
              mRequestingLocationUpdates = false;
          }

          updateUI();
        }
      });
  }

  /**
   * Updates all UI fields.
   */
  private void updateUI()
  {
    setButtonsEnabledState();
    updateLocationUI();
  }

  /**
   * Disables both buttons when functionality is disabled due to insuffucient location settings.
   * Otherwise ensures that only one button is enabled at any time. The Start Updates button is
   * enabled if the user is not requesting location updates. The Stop Updates button is enabled
   * if the user is requesting location updates.
   */
  private void setButtonsEnabledState()
  {
    if (mRequestingLocationUpdates)
    {
      mStartStopButton.setText(R.string.stop_updates);
    } else
    {
      mStartStopButton.setText(R.string.start_updates);
    }
  }

  /**
   * Sets the value of the UI fields for the location latitude, longitude and last update time.
   */
  private void updateLocationUI()
  {
    if (mCurrentLocation != null)
    {
      if (mStartAddress.length() < 1)
      {
        simpleChronometer.setBase(SystemClock.elapsedRealtime());
        simpleChronometer.start();
        getStartData();
      }

      double mSpeed;
      float currentSpeed = mCurrentLocation.getSpeed();

      switch (mPreference)
      {
        case "mph":
          mSpeed = (double) currentSpeed * mph;
          break;
        case "kph":
          mSpeed = (double) currentSpeed * kph;
          break;
        default:
          mSpeed = (double) currentSpeed;
          break;
      }

      if (mSpeed > topSpeed)
      {
        topSpeed = mSpeed;
      }
      if (mSpeed != 0)
      {
        this.speedArray.add(mSpeed);
      }

      String speedStr = String.format(Locale.getDefault(), "%.2f", mSpeed);
      this.mSpeedTextView.setText(speedStr);
    }
  }

  private void getStartData()
  {
    mStartTime = utils.getCurrentDateTime();
    mStartLatitude = mCurrentLocation.getLatitude();
    mStartLongitude = mCurrentLocation.getLongitude();

    url = "http://maps.googleapis.com/maps/api/geocode/json?latlng="+ mStartLatitude + "," + mStopLongitude;

    new GetApiData().execute();
  }

  /**
   * Removes location updates from the FusedLocationApi.
   */
  private void stopLocationUpdates()
  {
    if (!mRequestingLocationUpdates)
    {
      Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
      return;
    }

    // It is a good practice to remove location requests when the activity is in a paused or
    // stopped state. Doing so helps battery performance and is especially
    // recommended in applications that request frequent location updates.
    mFusedLocationClient.removeLocationUpdates(mLocationCallback)
      .addOnCompleteListener(this, new OnCompleteListener<Void>()
      {
        @Override
        public void onComplete(@NonNull Task<Void> task)
        {
          mRequestingLocationUpdates = false;
          setButtonsEnabledState();
        }
      });
  }

  @Override
  public void onResume()
  {
    super.onResume();
    // Within {@code onPause()}, we remove location updates. Here, we resume receiving
    // location updates if the user has requested them.
    if (mRequestingLocationUpdates && checkPermissions())
    {
      startLocationUpdates();
    } else if (!checkPermissions())
    {
      requestPermissions();
    }

    updateUI();
  }

  @Override
  protected void onPause()
  {
    super.onPause();

    // Remove location updates to save battery.
    stopLocationUpdates();
  }

  /**
   * Stores activity data in the Bundle.
   */
  public void onSaveInstanceState(Bundle savedInstanceState)
  {
    savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
    savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
    savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
    super.onSaveInstanceState(savedInstanceState);
  }


  /**
   * Shows a {@link Snackbar}.
   *
   * @param mainTextStringId The id for the string resource for the Snackbar text.
   * @param actionStringId   The text of the action item.
   * @param listener         The listener associated with the Snackbar action.
   */
  private void showSnackbar(final int mainTextStringId, final int actionStringId,
                            View.OnClickListener listener)
  {
    Snackbar.make(findViewById(android.R.id.content),
      getString(mainTextStringId),
      Snackbar.LENGTH_INDEFINITE)
      .setAction(getString(actionStringId), listener).show();
  }

  /**
   * Return the current state of the permissions needed.
   */
  private boolean checkPermissions()
  {
    int permissionState = ActivityCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_FINE_LOCATION);
    return permissionState == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissions()
  {
    boolean shouldProvideRationale =
      ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.ACCESS_FINE_LOCATION);

    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    if (shouldProvideRationale)
    {
      Log.i(TAG, "Displaying permission rationale to provide additional context.");

      showSnackbar(R.string.permission_rationale, android.R.string.ok,
        new View.OnClickListener()
        {
          @Override
          public void onClick(View view)
          {
            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this,
              new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
              REQUEST_PERMISSIONS_REQUEST_CODE);
          }
        });

    } else
    {
      Log.i(TAG, "Requesting permission");
      // Request permission. It's possible this can be auto answered if device policy
      // sets the permission in a given state or the user denied the permission
      // previously and checked "Never ask again".
      ActivityCompat.requestPermissions(MainActivity.this,
        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
        REQUEST_PERMISSIONS_REQUEST_CODE);
    }
  }

  /**
   * Callback received when a permissions request has been completed.
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults)
  {
    Log.i(TAG, "onRequestPermissionResult");
    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE)
    {
      if (grantResults.length <= 0)
      {
        // If user interaction was interrupted, the permission request is cancelled and you
        // receive empty arrays.
        Log.i(TAG, "User interaction was cancelled.");
      } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        // Permission granted.
      } else
      {
        // Permission denied.

        // Notify the user via a SnackBar that they have rejected a core permission for the
        // app, which makes the Activity useless. In a real app, core permissions would
        // typically be best requested during a welcome-screen flow.

        // Additionally, it is important to remember that a permission might have been
        // rejected without asking the user for permission (device policy or "Never ask
        // again" prompts). Therefore, a user interface affordance is typically implemented
        // when permissions are denied. Otherwise, your app could appear unresponsive to
        // touches or interactions which have required permissions.
        showSnackbar(R.string.permission_denied_explanation, R.string.settings,
          new View.OnClickListener()
          {
            @Override
            public void onClick(View view)
            {
              // Build intent that displays the App settings screen.
              Intent intent = new Intent();
              intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
              intent.setData(uri);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }
          });
      }
    }
  }


//  public void passData()
//  {
//    HashMap<String, String> hashMap = new HashMap<>();
//
//    hashMap.put("start_time", mStartTime);
//    hashMap.put("start_address", mStartAddress);
//    hashMap.put("start_lat", String.valueOf(mStartLatitude));
//    hashMap.put("start_lng", String.valueOf(mStartLongitude));
//
//    hashMap.put("stop_time", mStopTime);
//    String mStopAddress = "";
//    hashMap.put("stop_address", mStopAddress);
//    hashMap.put("stop_lat", String.valueOf(mStopLatitude));
//    hashMap.put("stop_lng", String.valueOf(mStopLongitude));
//
//    hashMap.put("total_time", mTotalTime);
//
//    double mAverage = utils.calculateAverage(speedArray);
//    String avgSpeed = String.format(Locale.getDefault(), "%.2f", mAverage);
//    hashMap.put("avg_speed", avgSpeed);
//
//    String maxSpeed = String.format(Locale.getDefault(), "%.2f", topSpeed);
//    hashMap.put("max_speed", String.valueOf(maxSpeed));
//
//    Intent intent = new Intent(this, TripResult.class);
//    intent.putExtra("map", hashMap);
//    startActivity(intent);
//  }

  ////////////////////////////////   test

  /**
   * Async task class to get json by making HTTP call
   */
  private class GetApiData extends AsyncTask<Void, Void, Void>
  {

//    @Override
//    protected void onPreExecute()
//    {
//      super.onPreExecute();
//    }

    @Override
    protected Void doInBackground(Void... arg0)
    {
      HttpHandler sh = new HttpHandler();

      // Making a request to url and getting response
      String jsonStr = sh.makeServiceCall(url);

      if (jsonStr != null)
      {
        try
        {
          JSONObject jsonObj = new JSONObject(jsonStr);

          // Getting JSON Array node
          JSONArray locationData = jsonObj.getJSONArray("results");

          // looping through all data
          for (int i = 0; i < locationData.length(); i++)
          {
            JSONObject c = locationData.getJSONObject(1);

            String foundAddress = c.getString("formatted_address");
            if(startAddess.equals(""))
            {
              startAddess = foundAddress;
            }
            else
            {
              endAddress = foundAddress;
            }
            Log.w("xxxxxx",""+foundAddress);
          }
        }
        catch (final JSONException e)
        {
          Log.e(TAG, "Json parsing error: " + e.getMessage());
        }
      } else
      {
        Log.e(TAG, "Couldn't get json from server.");
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result)
    {
      super.onPostExecute(result);

      passData();
    }

    }

  public void passData()
  {
    HashMap<Object, Object> hashMap = new HashMap<>();

    hashMap.put("start_time", mStartTime);
    hashMap.put("start_address", mStartAddress);
    hashMap.put("start_lat", mStartLatitude);
    hashMap.put("start_lng", mStartLongitude);

    hashMap.put("stop_time", mStopTime);
    hashMap.put("stop_address", endAddress);
    hashMap.put("stop_lat", mStopLatitude);
    hashMap.put("stop_lng", mStopLongitude);

    hashMap.put("total_time", mTotalTime);

    double mAverage = utils.calculateAverage(speedArray);
    String avgSpeed = String.format(Locale.getDefault(), "%.2f", mAverage);
    hashMap.put("avg_speed", avgSpeed);

    String maxSpeed = String.format(Locale.getDefault(), "%.2f", topSpeed);
    hashMap.put("max_speed", maxSpeed);

    Intent intent = new Intent(this, TripResult.class);
    intent.putExtra("map", hashMap);
    startActivity(intent);
  }

  }

