package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Random;

import static android.R.id.list;

public class CountryListActivity extends AppCompatActivity {

    private ListView mListviewCountries;

    private static final int REQ_STREET_ACTIVITY = 101;

    static final String EXTRA_SELECTED_COUNTRY_CODE = "COUNTRY_CODE";
    static final String EXTRA_RANDOM_COUNTRY = "RANDOM_COUNTRY";

    static final String[] countryNames = {
            "World",
            "Albania",
            "Androrra",
            "Argentina",
            "Australia",
            "Bangladesh",
            "Belgium",
            "Bhutan",
            "Bolivia",
            "Brazil",
            "Bulgaria",
            "Cambodia",
            "Canada",
            "Chile",
            "Colombia",
            "Croatia",
            "Czech Republic",
            "Denmark",
            "Ecuador",
            "Estonia",
            "Finland",
            "France",
            "Germany",
            "Ghana",
            "Greece",
            "Hungary",
            "Iceland",
            "Indonesia",
            "Ireland",
            "Israel",
            "Italy",
            "Japan",
            "Kyrgyzstan",
            "Latvia",
            "Lesotho",
            "Lithuania",
            "Luxembourg",
            "Macedonia",
            "Malaysia",
            "Mexico",
            "Mongolia",
            "Montenegro",
            "Netherlands",
            "New Zealand",
            "Peru",
            "Philippines",
            "Poland",
            "Portugal",
            "Puerto Rico",
            "Romania",
            "Senegal",
            "Serbia",
            "Singapore",
            "Slovakia",
            "Slovenia",
            "South Africa",
            "South Korea",
            "Spain",
            "Sri Lanka",
            "Swaziland",
            "Sweden",
            "Switzerland",
            "Taiwan",
            "Thailand",
            "Tunisia",
            "Turkey",
            "Uganda",
            "Ukraine",
            "United Kingdom",
            "United States",
            "Uruguay"
    };

    static final String[] countryCodes = {
            "AL",
            "AD",
            "AR",
            "AU",
            "BD",
            "BE",
            "BT",
            "BO",
            "BR",
            "BG",
            "KH",
            "CA",
            "CL",
            "CO",
            "HR",
            "CZ",
            "DK",
            "EC",
            "EE",
            "FI",
            "FR",
            "DE",
            "GH",
            "GR",
            "HU",
            "IS",
            "ID",
            "IE",
            "IL",
            "IT",
            "JP",
            "KG",
            "LV",
            "LS",
            "LT",
            "LU",
            "MK",
            "MY",
            "MX",
            "MN",
            "ME",
            "NL",
            "NZ",
            "PE",
            "PH",
            "PL",
            "PT",
            "PR",
            "RO",
            "SN",
            "RS",
            "SG",
            "SK",
            "SI",
            "ZA",
            "KR",
            "ES",
            "LK",
            "SZ",
            "SE",
            "CH",
            "TW",
            "TH",
            "TN",
            "TR",
            "UG",
            "UA",
            "GB",
            "US",
            "UY"
    };

    static final Integer[] imgIDs = {
            R.drawable.world,
            R.drawable.albania,
            R.drawable.andorra,
            R.drawable.argentina,
            R.drawable.australia,
            R.drawable.bangladesh,
            R.drawable.belgium,
            R.drawable.bhutan,
            R.drawable.bolivia,
            R.drawable.brazil,
            R.drawable.bulgaria,
            R.drawable.cambodja,
            R.drawable.canada,
            R.drawable.chile,
            R.drawable.colombia,
            R.drawable.croatia,
            R.drawable.czech_republic,
            R.drawable.denmark,
            R.drawable.ecuador,
            R.drawable.estonia,
            R.drawable.finland,
            R.drawable.france,
            R.drawable.germany,
            R.drawable.ghana,
            R.drawable.greece,
            R.drawable.hungary,
            R.drawable.iceland,
            R.drawable.indonesia,
            R.drawable.ireland,
            R.drawable.israel,
            R.drawable.italy,
            R.drawable.japan,
            R.drawable.kyrgyzstan,
            R.drawable.latvia,
            R.drawable.lesotho,
            R.drawable.lithuania,
            R.drawable.luxembourg,
            R.drawable.macedonia,
            R.drawable.malaysia,
            R.drawable.mexico,
            R.drawable.mongolia,
            R.drawable.montenegro,
            R.drawable.netherlands,
            R.drawable.new_zealand,
            R.drawable.peru,
            R.drawable.philippines,
            R.drawable.poland,
            R.drawable.portugal,
            R.drawable.puerto_rico,
            R.drawable.romania,
            R.drawable.senegal,
            R.drawable.serbia,
            R.drawable.singapore,
            R.drawable.slovakia,
            R.drawable.slovenia,
            R.drawable.south_africa,
            R.drawable.south_korea,
            R.drawable.spain,
            R.drawable.sri_lanka,
            R.drawable.swaziland,
            R.drawable.sweden,
            R.drawable.switzerland,
            R.drawable.taiwan,
            R.drawable.thailand,
            R.drawable.tunisia,
            R.drawable.turkey,
            R.drawable.uganda,
            R.drawable.ukraine,
            R.drawable.uk,
            R.drawable.usa,
            R.drawable.uruguay
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_list);

        CountryListAdapter adapter = new CountryListAdapter(this, countryNames, imgIDs);
        mListviewCountries = (ListView) findViewById(R.id.lv_countryList);
        mListviewCountries.setAdapter(adapter);

        mListviewCountries.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean randomCountry = false;
                Intent intent = new Intent(CountryListActivity.this, StreetViewActivity.class);

                if (position != 0) { //start streetViewActivity with country code.
                    String selectedCountryCode = countryCodes[position - 1];
                    intent.putExtra(EXTRA_SELECTED_COUNTRY_CODE, selectedCountryCode);
                } else { //start streetViewActivity with info that it needs to choose random country code every time
                    randomCountry = true;
                }
                intent.putExtra(EXTRA_RANDOM_COUNTRY, randomCountry);
                startActivityForResult(intent, REQ_STREET_ACTIVITY);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_STREET_ACTIVITY:
                if (data != null) {
                    Bundle resultData = data.getExtras();
                    int score = resultData.getInt(StreetViewActivity.RESULT_KEY_SCORE);
                    Toast.makeText(this, Integer.toString(score), Toast.LENGTH_LONG).show(); //TODO: replace with screen presenting score
                }
                break;
        }
    }

    static String getRandomCode() {
        Random rand = new Random(System.currentTimeMillis());
        int codeIndex = rand.nextInt(countryCodes.length);
        return countryCodes[codeIndex];
    }
}
