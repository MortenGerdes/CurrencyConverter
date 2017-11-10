package dk.au.morten.currencyconverter2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    boolean isStatic = false;
    private Timer timer = null;
    private NetworkStuff ns = new NetworkStuff();
    private HashMap<String, Double> staticRates = new HashMap<>();
    private ListView lv = null;

    private String[] values = new String[] {
            "1.42",
            "52.52",
            "5346.23",
            "2345.00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        populateRateMap();
        ns.execute("https://api.fixer.io/latest");
    }

    @Override
    protected void onStart() {
        super.onStart();

        lv = (ListView) findViewById(R.id.HistoryView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        lv.setAdapter(adapter);

        // ListView Item Click Listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                                      @Override
                                      public void onItemClick(AdapterView<?> parent, View view,
                                                              int position, long id) {

                                          // ListView Clicked item index
                                          int itemPosition = position;

                                          // ListView Clicked item value
                                          String itemValue = (String) lv.getItemAtPosition(position);

                                          // Show Alert
                                          Toast.makeText(getApplicationContext(),
                                                  "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
                                                  .show();

                                      }
                                  });






        JSONObject jObj = null;
        timer = new Timer();
        try {
            jObj = ns.get(10, TimeUnit.SECONDS);
            Log.d("CurrencyConverter", "Proper internet connection detected. Getting online values!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            Log.d("CurrencyConverter", "Connection timeout. Using static rates!");
        }

        if(jObj == null)
        {
            isStatic = true;
            Iterator it = staticRates.entrySet().iterator();
            jObj = new JSONObject();
            while(it.hasNext())
            {
                Map.Entry pairs = (Map.Entry)it.next();
                try {
                    jObj.put((String) pairs.getKey(), pairs.getValue());
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
            Toast.makeText(getApplicationContext(),
                    "No internet connection could be made. Using static values.", Toast.LENGTH_LONG)
                    .show();
        }
        else
        {
            Toast.makeText(getApplicationContext(),
                    "Internet connection found. Using latest values", Toast.LENGTH_LONG)
                    .show();
        }

        final Spinner fromSpinner = (Spinner)findViewById(R.id.fromDD);
        final Spinner toSpinner = (Spinner)findViewById(R.id.toDD);
        setDropDownAdapter((Spinner)findViewById(R.id.fromDD));
        setDropDownAdapter((Spinner)findViewById(R.id.toDD));
        fromSpinner.setSelection(4);
        toSpinner.setSelection(0);

        final JSONObject finalJObj = jObj;
        timer.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run()
                                  {
                                      final TextView toText = (TextView)findViewById(R.id.toText);
                                      toText.setEnabled(false);

                                      TextView fromText = (TextView)findViewById(R.id.fromText);
                                      double exRateFrom = 0;
                                      double exRateTo = 0;
                                      double result = 0;

                                      try {
                                          JSONObject ja = (isStatic) ? finalJObj : finalJObj.getJSONObject("rates");
                                          ja.put("EUR", 1.00);
                                          exRateFrom = ja.getDouble(fromSpinner.getSelectedItem().toString());
                                          exRateTo = ja.getDouble(toSpinner.getSelectedItem().toString());
                                      } catch (JSONException e) {
                                          e.printStackTrace();
                                      }
                                      if(!fromText.getText().toString().isEmpty())
                                      {
                                          result = (Double.valueOf(fromText.getText().toString()) / exRateFrom) * exRateTo;
                                      }
                                      final double finalResult = result;
                                      new Handler(Looper.getMainLooper()).post(new Runnable() {
                                          @Override
                                          public void run() {
                                              toText.setText("" + round(finalResult, 2), TextView.BufferType.EDITABLE);
                                          }
                                      });
                                      Log.d("CurrencyConverter", "Result = " + result + toSpinner.getSelectedItem().toString());
                                  }

                              },
                0,
                200);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    public void switchValues(View v)
    {
        Spinner fromSpinner = (Spinner)findViewById(R.id.fromDD);
        Spinner toSpinner = (Spinner)findViewById(R.id.toDD);
        int fromSpinnerValue = fromSpinner.getSelectedItemPosition();
        int toSpinnerValue = toSpinner.getSelectedItemPosition();

        fromSpinner.setSelection(toSpinnerValue);
        toSpinner.setSelection(fromSpinnerValue);
    }

    private void setDropDownAdapter(Spinner spinner)
    {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currency_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void populateRateMap()
    {
        staticRates.put("AUD", 1.5184);
        staticRates.put("BGN", 1.9558);
        staticRates.put("BRL", 3.8012);
        staticRates.put("CAD", 1.483);
        staticRates.put("CHF", 1.1635);
        staticRates.put("CNY", 7.72);
        staticRates.put("CZK", 25.652);
        staticRates.put("DKK", 7.4421);
        staticRates.put("GBP", 0.88923);
        staticRates.put("HKD", 9.0953);
        staticRates.put("HRK", 7.5335);
        staticRates.put("HUF", 310.29);
        staticRates.put("IDR", 15729.00);
        staticRates.put("ILS", 4.0839);
        staticRates.put("INR", 75.214);
        staticRates.put("JPY", 132.82);
        staticRates.put("KRW", 1298.7);
        staticRates.put("MXN", 22.142);
        staticRates.put("MYR", 4.9332);
        staticRates.put("NOK", 9.4843);
        staticRates.put("NZD", 1.68);
        staticRates.put("PHP", 59.52);
        staticRates.put("PLN", 4.238);
        staticRates.put("RON", 4.59);
        staticRates.put("RUB", 68.295);
        staticRates.put("SEK", 9.7912);
        staticRates.put("SGD", 1.585);
        staticRates.put("THB", 38.60);
        staticRates.put("TRY", 4.4927);
        staticRates.put("USD", 1.16);
        staticRates.put("ZAR", 16.46);
    }

}
