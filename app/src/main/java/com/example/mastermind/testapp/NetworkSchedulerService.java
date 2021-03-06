package com.example.mastermind.testapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nex3z.notificationbadge.NotificationBadge;
import com.txusballesteros.bubbles.BubbleLayout;
import com.txusballesteros.bubbles.BubblesManager;
import com.txusballesteros.bubbles.BubblesService;
import com.txusballesteros.bubbles.OnInitializedCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mastermind on 9/5/2018.
 */

public class NetworkSchedulerService extends JobService implements
        ConnectivityReceiver.ConnectivityReceiverListener {

    BubblesManager bubblesManager;
    NotificationCompat.Builder notification;
    private static final int uniqueID = 45612;
    Date currentTime = new Date();
    Date lastUpdate;
    ArrayList<JobOffer> asyncOffers = new ArrayList<>();
    int notCount;
    SharedPreferences settingsPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.getAppContext());


    ArrayList<BubbleLayout> bubbles;

    boolean bubbleInit = false;
    boolean bubbleDel = false;
    NotificationManager nm;
    NotificationBadge mBadge;
    ArrayList<Integer> idArray = new ArrayList<>();
    String message = "";
    RequestQueue queue;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    private static final String TAG = NetworkSchedulerService.class.getSimpleName();

    private ConnectivityReceiver mConnectivityReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        mConnectivityReceiver = new ConnectivityReceiver(this);

    }



    /**
     * When the app's NetworkConnectionActivity is created, it starts this service. This is so that the
     * activity and this service can communicate back and forth. See "setUiCallback()"
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }




    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "onStartJob" + mConnectivityReceiver);
        registerReceiver(mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));



        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "onStopJob");
        unregisterReceiver(mConnectivityReceiver);
        return true;
    }


    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

        String message = isConnected ? "Good! Connected to Internet" : "Sorry! Not connected to internet";
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

        notCount = 0;

        queue = Volley.newRequestQueue(MyApplication.getAppContext());
        if(isConnected && settingsPreferences.getBoolean("makeRequest",true)) {

            for (int j = 0; j < settingsPreferences.getInt("numberOfCheckedCategories", 0); j++) {

                queue.add(volleySetCheckedCategories(String.valueOf(settingsPreferences.getInt("checkedCategoryId " + j, 0))));
                //new TaskShowOffersFromCategories().execute(String.valueOf(settingsPreferences.getInt("checkedCategoryId " + j, 0)));

            }
        }

    }



    public int checkForOffers() {
        boolean searchChecked = false;
        int notCount = 0;
        for (int i = 0; i < settingsPreferences.getInt("numberOfOffers", 0); i++) {
            System.out.println(settingsPreferences.getInt("numberOfOffers", 0));
            System.out.println(settingsPreferences.getInt("numberOfCheckedCategories", 0));
            System.out.println(settingsPreferences.getLong("offerDate " + i,0) > settingsPreferences.getLong("lastSeenDate", 0));
            if (settingsPreferences.getLong("offerDate " + i,0) > settingsPreferences.getLong("lastSeenDate", 0)) {
                for(int j = 0; j < settingsPreferences.getInt("numberOfCheckedCategories", 0); j++) {
                    System.out.println(settingsPreferences.getInt("offerCatid " + i,0));
                    System.out.println(settingsPreferences.getInt("checkedCategoryId "+j,0));
                    if(settingsPreferences.getInt("offerCatid " + i,0)==settingsPreferences.getInt("checkedCategoryId "+j,0)) {
                        notCount++;

                    }
                }
            }

            System.out.println(settingsPreferences.getLong("lastSeenDate", 0) + " at the end of alarmreceiver ");

        }

        return notCount;
    }

    public StringRequest volleySetCheckedCategories(final String param) {
        String url = "http://10.0.2.2/android/jobAds.php?";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // Display the first 500 characters of the response string.
                        System.out.println("Volley: " + message);
                        try {
                            JSONObject jsonObjectAll = new JSONObject(response);
                            JSONArray jsonArray = jsonObjectAll.getJSONArray("offers");
                            int i = 0;

                            while (i < jsonArray.length() && i < 5) {


                                JSONObject jsonObjectCategory = jsonArray.getJSONObject(i);

                                if(!idArray.contains(Integer.valueOf(jsonObjectCategory.getString("jad_id")))) {
                                    idArray.add(Integer.valueOf(jsonObjectCategory.getString("jad_id")));


                                    JobOffer offer = new JobOffer();
                                    offer.setId(Integer.valueOf(jsonObjectCategory.getString("jad_id")));
                                    offer.setCatid(Integer.valueOf(jsonObjectCategory.getString("jad_catid")));
                                    offer.setTitle(jsonObjectCategory.getString("jad_title"));
                                    offer.setDate(format.parse(jsonObjectCategory.getString("jad_date")));
                                    offer.setDownloaded(jsonObjectCategory.getString("jad_downloaded"));
                                    System.out.println(offer.getTitle() + " first time");

                                    asyncOffers.add(offer);

                                    Collections.sort(asyncOffers, new Comparator<JobOffer>() {
                                        @Override
                                        public int compare(JobOffer jobOffer, JobOffer t1) {
                                            if (jobOffer.getDate().getTime() - t1.getDate().getTime() < 0)
                                                return 1;
                                            else if (jobOffer.getDate().getTime() - t1.getDate().getTime() == 0)
                                                return 0;
                                            else
                                                return -1;
                                        }
                                    });
                                    for (int x = 0; x < asyncOffers.size(); x++) {
                                        System.out.println(asyncOffers.get(x).getTitle());
                                    }
                                }

                                i++;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }



                        for (int j = 0; j < 5; j++) {
                            settingsPreferences.edit().remove("offerId " + j).apply();
                            settingsPreferences.edit().remove("offerCatid " + j).apply();
                            settingsPreferences.edit().remove("offerTitle " + j).apply();
                            settingsPreferences.edit().remove("offerDate " + j).apply();
                            settingsPreferences.edit().remove("offerDownloaded " + j).apply();
                        }

                        if(asyncOffers.size()>0) {
                            for (int i = 0; i < asyncOffers.size(); i++) {
                                if (i < 5) {

                                    settingsPreferences.edit().putInt("offerId " + i, asyncOffers.get(i).getId()).apply();
                                    settingsPreferences.edit().putInt("offerCatid " + i, asyncOffers.get(i).getCatid()).apply();
                                    settingsPreferences.edit().putString("offerTitle " + i, asyncOffers.get(i).getTitle()).apply();
                                    settingsPreferences.edit().putLong("offerDate " + i, asyncOffers.get(i).getDate().getTime()).apply();
                                    settingsPreferences.edit().putString("offerDownloaded " + i, asyncOffers.get(i).getDownloaded()).apply();
                                    System.out.println(settingsPreferences.getLong("offerDate " + i, 0));
                                    System.out.println(settingsPreferences.getString("offerTitle " + i, ""));
                                    settingsPreferences.edit().putInt("numberOfOffers", asyncOffers.size()).apply();
                                } else
                                    settingsPreferences.edit().putInt("numberOfOffers", 5).apply();
                            }
                            System.out.println(settingsPreferences.getLong("lastSeenDate", 0));


                            if (checkForOffers() > 0 && asyncOffers.get(0).getDate().getTime() > settingsPreferences.getLong("lastNotDate", 0)) {

                                initializeBubblesManager();


                                settingsPreferences.edit().putInt("numberOfUnseenOffers", checkForOffers()).apply();
                                System.out.println(settingsPreferences.getInt("numberOfUnseenOffers", 0));



                                notification = new NotificationCompat.Builder(MyApplication.getAppContext(), "notification");
                                notification.setAutoCancel(true);

                                notification.setSmallIcon(R.drawable.ic_not);
                                notification.setTicker("This is the ticker");
                                notification.setWhen(System.currentTimeMillis());
                                notification.setCategory(NotificationCompat.CATEGORY_REMINDER);
                                notification.setDefaults(Notification.DEFAULT_ALL);
                                notification.setPriority(Notification.PRIORITY_MAX);
                                notification.setVibrate(new long[0]);
                                notification.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);

                                Intent intentBackToMain = new Intent(MyApplication.getAppContext(), UnseenActivity.class);
                                intentBackToMain.putExtra("source", "alarm");
                                PendingIntent pendingIntent = PendingIntent.getActivity(MyApplication.getAppContext(), 0, intentBackToMain, PendingIntent.FLAG_UPDATE_CURRENT);
                                notification.setContentTitle("You have " + checkForOffers() + " unseen offers!!");
                                notification.setContentIntent(pendingIntent);

                                nm = (NotificationManager) MyApplication.getAppContext().getSystemService(NOTIFICATION_SERVICE);
                                nm.notify(uniqueID, notification.build());




                                settingsPreferences.edit().putBoolean("makeRequest", false).apply();
                                settingsPreferences.edit().putLong("lastNotDate", asyncOffers.get(0).getDate().getTime()).apply();

                            } else {
                                Toast.makeText(MyApplication.getAppContext(), "There is some problem with the server", Toast.LENGTH_LONG);
                            }
                        }

                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    message = "TimeOutError";
                    //This indicates that the reuest has either time out or there is no connection

                } else if (error instanceof AuthFailureError) {
                    message = "AuthFailureError";
                    // Error indicating that there was an Authentication Failure while performing the request

                } else if (error instanceof ServerError) {
                    message = "ServerError";
                    //Indicates that the server responded with a error response

                } else if (error instanceof NetworkError) {
                    message = "NetworkError";
                    //Indicates that there was network error while performing the request

                } else if (error instanceof ParseError) {
                    message = "ParseError";
                    // Indicates that the server response could not be parsed

                }
                System.out.println("Volley: " + message);
                if(!message.equals("")){
                    Toast.makeText(MyApplication.getAppContext(),"There is some problem with the server ("+message+")",Toast.LENGTH_LONG).show();
                }
            }
        }
        )
        {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "showOffersFromCategory");
                params.put("jacat_id",param);

                return params;
            }
        };
        return stringRequest;
    }

    //This method is executed to add a new bubble.
    private void addNewBubble() {
        BubbleLayout bubbleView = (BubbleLayout)LayoutInflater.from(MyApplication.getAppContext()).inflate(R.layout.bubble_layout, null);
        mBadge =bubbleView.findViewById(R.id.badge);
        mBadge.setNumber(checkForOffers());

        bubbleView.setOnBubbleRemoveListener(new BubbleLayout.OnBubbleRemoveListener() {
            @Override
            public void onBubbleRemoved(BubbleLayout bubble) { }
        });

        //The Onclick Listener for the bubble has been set below.
        bubbleView.setOnBubbleClickListener(new BubbleLayout.OnBubbleClickListener() {

            @Override
            public void onBubbleClick(BubbleLayout bubble) {

                Toast.makeText(MyApplication.getAppContext(), "Clicked", Toast.LENGTH_SHORT).show();
                Intent intentBubbleToMain = new Intent(MyApplication.getAppContext(), UnseenActivity.class);
                intentBubbleToMain.putExtra("source", "alarm");
                MyApplication.getAppContext().startActivity(intentBubbleToMain);
                bubblesManager.removeBubble(bubble);

                bubbleDel = true;
                nm.cancel(uniqueID);
            }
        });
        bubbleView.setShouldStickToWall(true);
        bubblesManager.addBubble(bubbleView, 60, 20);
    }

    private void initializeBubblesManager() {
        bubblesManager = new BubblesManager.Builder(this)
                .setTrashLayout(R.layout.bubble_remove)
                .setInitializationCallback(new OnInitializedCallback() {
                    @Override
                    public void onInitialized() {
                        addNewBubble();
                    }
                })
                .build();
        bubblesManager.initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bubblesManager.recycle();
    }
}

