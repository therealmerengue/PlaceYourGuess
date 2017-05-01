package holders;

import com.example.trm.placeyourguess.R;

import java.util.Random;

public class LocationListItemsHolder {

    public static final String[] mCountryNames = {
            "World",
            "Custom",
            "Large cities",
            "Albania",
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
    public static final String[] mCountryCodes = {
            "AL",
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
    public static final Integer[] mImgIDs = {
            R.drawable.world,
            R.drawable.help_black,
            R.drawable.city,
            R.drawable.albania,
            R.drawable.argentina,
            R.drawable.australia,
            R.drawable.bangladesh,
            R.drawable.belgium,
            R.drawable.bhutan,
            R.drawable.bolivia,
            R.drawable.brazil,
            R.drawable.bulgaria,
            R.drawable.cambodia,
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

    public static String getRandomCode() {
        Random rand = new Random(System.currentTimeMillis());
        int codeIndex = rand.nextInt(mCountryCodes.length);
        return mCountryCodes[codeIndex];
    }
}
