package com.prym.backend.util;

import java.util.HashMap;
import java.util.Map;

/**
 *Utility class for ZIP/postal code validation and coordinate lookup
 *Supports both US ZIP codes (12345) and Canadian postal codes (A1A 1A1)
 *For MVP, contains hardcoded coordinates for major US and Canadian cities
 *Can be expanded with full database from SimpleMaps or GeoNames
 */
public class ZipCodeUtil {

    private static final Map<String, double[]> ZIP_TO_COORDS = new HashMap<>();
    private static final Map<String, double[]> POSTAL_TO_COORDS = new HashMap<>();

    static {
        //CANADIAN POSTAL CODES
        //Ontario
        POSTAL_TO_COORDS.put("M5H2N2", new double[]{43.6532, -79.3832}); // Toronto - Downtown
        POSTAL_TO_COORDS.put("M5V3A8", new double[]{43.6426, -79.3871}); // Toronto - Entertainment District
        POSTAL_TO_COORDS.put("M4Y1J8", new double[]{43.6658, -79.3832}); // Toronto - Church-Wellesley
        POSTAL_TO_COORDS.put("M5T1R8", new double[]{43.6532, -79.3957}); // Toronto - Kensington Market
        POSTAL_TO_COORDS.put("M5S2E4", new double[]{43.6629, -79.3957}); // Toronto - University of Toronto
        POSTAL_TO_COORDS.put("M6G3E6", new double[]{43.6689, -79.4229}); // Toronto - Christie Pits
        POSTAL_TO_COORDS.put("M6J1M3", new double[]{43.6479, -79.4197}); // Toronto - Liberty Village
        POSTAL_TO_COORDS.put("M6K1X9", new double[]{43.6368, -79.4281}); // Toronto - Parkdale
        POSTAL_TO_COORDS.put("M6R2B5", new double[]{43.6489, -79.4560}); // Toronto - High Park
        POSTAL_TO_COORDS.put("M4E3W2", new double[]{43.6763, -79.2930}); // Toronto - The Beaches
        POSTAL_TO_COORDS.put("M4L3T4", new double[]{43.6689, -79.3155}); // Toronto - Leslieville
        POSTAL_TO_COORDS.put("M4M2B8", new double[]{43.6595, -79.3402}); // Toronto - Riverside
        POSTAL_TO_COORDS.put("M4C5L6", new double[]{43.6953, -79.3183}); // Toronto - East York
        POSTAL_TO_COORDS.put("M4N3M5", new double[]{43.7280, -79.3887}); // Toronto - North York
        POSTAL_TO_COORDS.put("M2N5P5", new double[]{43.7701, -79.4085}); // Toronto - Willowdale
        POSTAL_TO_COORDS.put("M3C1B5", new double[]{43.7258, -79.3401}); // Toronto - Don Mills
        POSTAL_TO_COORDS.put("M1B5K7", new double[]{43.8066, -79.1943}); // Toronto - Scarborough
        POSTAL_TO_COORDS.put("M9A1B1", new double[]{43.6679, -79.5322}); // Toronto - Etobicoke
        POSTAL_TO_COORDS.put("M9C1B8", new double[]{43.6435, -79.5772}); // Toronto - Etobicoke West
        POSTAL_TO_COORDS.put("K1A0A9", new double[]{45.4215, -75.6972}); // Ottawa - Downtown
        POSTAL_TO_COORDS.put("K1N8L7", new double[]{45.4277, -75.6903}); // Ottawa - ByWard Market
        POSTAL_TO_COORDS.put("K1P1J1", new double[]{45.4165, -75.6984}); // Ottawa - Centretown
        POSTAL_TO_COORDS.put("K1S1N4", new double[]{45.4008, -75.6903}); // Ottawa - Glebe
        POSTAL_TO_COORDS.put("K1Y4K7", new double[]{45.3945, -75.7377}); // Ottawa - Westboro
        POSTAL_TO_COORDS.put("K2P0A4", new double[]{45.4104, -75.7198}); // Ottawa - Hintonburg
        POSTAL_TO_COORDS.put("L5B1M2", new double[]{43.5890, -79.6441}); // Mississauga - Downtown
        POSTAL_TO_COORDS.put("L4Z1S2", new double[]{43.6532, -79.7118}); // Mississauga - Square One
        POSTAL_TO_COORDS.put("N2L3G1", new double[]{43.4643, -80.5204}); // Waterloo
        POSTAL_TO_COORDS.put("N2J3Z4", new double[]{43.4516, -80.4925}); // Kitchener
        POSTAL_TO_COORDS.put("N1H6J2", new double[]{43.5448, -80.2482}); // Guelph
        POSTAL_TO_COORDS.put("L6T5P5", new double[]{43.7315, -79.7624}); // Brampton
        POSTAL_TO_COORDS.put("L3R0E7", new double[]{43.8561, -79.3370}); // Markham
        POSTAL_TO_COORDS.put("L4L9N6", new double[]{43.8563, -79.5085}); // Vaughan
        POSTAL_TO_COORDS.put("L9T7T4", new double[]{43.5448, -79.7624}); // Milton

        //Quebec
        POSTAL_TO_COORDS.put("H2Y1C6", new double[]{45.5017, -73.5673}); // Montreal - Downtown
        POSTAL_TO_COORDS.put("H2X2H9", new double[]{45.5088, -73.5673}); // Montreal - Gay Village
        POSTAL_TO_COORDS.put("H2Z1B3", new double[]{45.5017, -73.5540}); // Montreal - Old Montreal
        POSTAL_TO_COORDS.put("H3A0G4", new double[]{45.5048, -73.5772}); // Montreal - McGill
        POSTAL_TO_COORDS.put("H3B4W8", new double[]{45.4995, -73.5694}); // Montreal - Place-des-Arts
        POSTAL_TO_COORDS.put("H3G1M8", new double[]{45.5089, -73.5734}); // Montreal - Plateau
        POSTAL_TO_COORDS.put("H2T1A8", new double[]{45.5276, -73.5965}); // Montreal - Mile End
        POSTAL_TO_COORDS.put("H2V4E4", new double[]{45.5322, -73.6186}); // Montreal - Outremont
        POSTAL_TO_COORDS.put("H4C1Z3", new double[]{45.4795, -73.5833}); // Montreal - Verdun
        POSTAL_TO_COORDS.put("H3H1P3", new double[]{45.4937, -73.5833}); // Montreal - NDG
        POSTAL_TO_COORDS.put("H1X3B7", new double[]{45.6066, -73.5693}); // Montreal - Anjou
        POSTAL_TO_COORDS.put("H8Y1W5", new double[]{45.4415, -73.6132}); // Montreal - LaSalle
        POSTAL_TO_COORDS.put("G1R4P5", new double[]{46.8139, -71.2080}); // Quebec City - Old Quebec
        POSTAL_TO_COORDS.put("G1K3Y5", new double[]{46.8123, -71.2145}); // Quebec City - Saint-Roch
        POSTAL_TO_COORDS.put("G1V4H6", new double[]{46.7616, -71.2713}); // Quebec City - Sainte-Foy
        POSTAL_TO_COORDS.put("J4H3X9", new double[]{45.5308, -73.5211}); // Longueuil
        POSTAL_TO_COORDS.put("J3Y8Y7", new double[]{45.4765, -73.4515}); // Saint-Hubert
        POSTAL_TO_COORDS.put("J7Y5E9", new double[]{45.5601, -73.7516}); // Laval

        // British Columbia
        POSTAL_TO_COORDS.put("V6B2W9", new double[]{49.2827, -123.1207}); // Vancouver - Downtown
        POSTAL_TO_COORDS.put("V6C3L7", new double[]{49.2865, -123.1207}); // Vancouver - Coal Harbour
        POSTAL_TO_COORDS.put("V6E4L9", new double[]{49.2722, -123.1315}); // Vancouver - West End
        POSTAL_TO_COORDS.put("V6G1Z6", new double[]{49.2606, -123.1496}); // Vancouver - Kitsilano
        POSTAL_TO_COORDS.put("V6K4W3", new double[]{49.2688, -123.1689}); // Vancouver - Point Grey
        POSTAL_TO_COORDS.put("V5K0A1", new double[]{49.2827, -123.0690}); // Vancouver - East Van
        POSTAL_TO_COORDS.put("V5L1K3", new double[]{49.2488, -123.0450}); // Vancouver - Grandview
        POSTAL_TO_COORDS.put("V5T1A1", new double[]{49.2659, -123.1040}); // Vancouver - Mount Pleasant
        POSTAL_TO_COORDS.put("V6A1A4", new double[]{49.2819, -123.1004}); // Vancouver - Gastown
        POSTAL_TO_COORDS.put("V6J1M9", new double[]{49.2659, -123.1496}); // Vancouver - Fairview
        POSTAL_TO_COORDS.put("V6R4J6", new double[]{49.2327, -123.1635}); // Vancouver - Dunbar
        POSTAL_TO_COORDS.put("V7Y1A1", new double[]{49.3200, -123.0724}); // North Vancouver
        POSTAL_TO_COORDS.put("V7L1C4", new double[]{49.3263, -123.0779}); // North Vancouver - Lonsdale
        POSTAL_TO_COORDS.put("V5H4M1", new double[]{49.1666, -122.7994}); // Burnaby - Metrotown
        POSTAL_TO_COORDS.put("V3L5N8", new double[]{49.2488, -122.8814}); // Burnaby - Brentwood
        POSTAL_TO_COORDS.put("V3J7E3", new double[]{49.2000, -122.9111}); // New Westminster
        POSTAL_TO_COORDS.put("V6X1R9", new double[]{49.1666, -123.1336}); // Richmond
        POSTAL_TO_COORDS.put("V7C4V4", new double[]{49.1666, -123.0724}); // Richmond - Steveston
        POSTAL_TO_COORDS.put("V3B7X8", new double[]{49.2819, -122.7931}); // Coquitlam
        POSTAL_TO_COORDS.put("V3H3W5", new double[]{49.2488, -122.8497}); // Port Moody
        POSTAL_TO_COORDS.put("V3K6R7", new double[]{49.2106, -122.7098}); // Port Coquitlam
        POSTAL_TO_COORDS.put("V3S0C4", new double[]{49.1059, -122.8650}); // Surrey - City Centre
        POSTAL_TO_COORDS.put("V4N0S7", new double[]{49.1913, -122.8490}); // Surrey - Guildford
        POSTAL_TO_COORDS.put("V8W2Y1", new double[]{48.4284, -123.3656}); // Victoria - Downtown
        POSTAL_TO_COORDS.put("V8V3K1", new double[]{48.4622, -123.3656}); // Victoria - Saanich
        POSTAL_TO_COORDS.put("V9A1A1", new double[]{48.4359, -123.3230}); // Victoria - Oak Bay

        //Alberta
        POSTAL_TO_COORDS.put("T2P0R4", new double[]{51.0447, -114.0719}); // Calgary - Downtown
        POSTAL_TO_COORDS.put("T2G0P6", new double[]{51.0375, -114.0719}); // Calgary - Beltline
        POSTAL_TO_COORDS.put("T2R1J5", new double[]{51.0486, -114.0931}); // Calgary - Eau Claire
        POSTAL_TO_COORDS.put("T2S3J2", new double[]{51.0375, -114.0931}); // Calgary - Mission
        POSTAL_TO_COORDS.put("T2N1N4", new double[]{51.0791, -114.1331}); // Calgary - Kensington
        POSTAL_TO_COORDS.put("T3B0K3", new double[]{51.1215, -114.0631}); // Calgary - Nose Hill
        POSTAL_TO_COORDS.put("T3K0V7", new double[]{51.1544, -114.0719}); // Calgary - Beddington
        POSTAL_TO_COORDS.put("T5J2R7", new double[]{53.5461, -113.4938}); // Edmonton - Downtown
        POSTAL_TO_COORDS.put("T6E2M7", new double[]{53.5232, -113.5244}); // Edmonton - University
        POSTAL_TO_COORDS.put("T5K2J1", new double[]{53.5677, -113.5244}); // Edmonton - Oliver
        POSTAL_TO_COORDS.put("T6G2E1", new double[]{53.5232, -113.4631}); // Edmonton - Bonnie Doon
        POSTAL_TO_COORDS.put("T1H3Z7", new double[]{49.6951, -112.8326}); // Lethbridge
        POSTAL_TO_COORDS.put("T4R2L9", new double[]{52.2681, -113.8111}); // Red Deer

        //Manitoba
        POSTAL_TO_COORDS.put("R3C4K8", new double[]{49.8951, -97.1384}); // Winnipeg - Downtown
        POSTAL_TO_COORDS.put("R3B0Y4", new double[]{49.8908, -97.1471}); // Winnipeg - Exchange District
        POSTAL_TO_COORDS.put("R3T2N2", new double[]{49.8089, -97.1384}); // Winnipeg - Fort Garry
        POSTAL_TO_COORDS.put("R3M3Y5", new double[]{49.8682, -97.1889}); // Winnipeg - River Heights
        POSTAL_TO_COORDS.put("R2C5N5", new double[]{49.9145, -97.0297}); // Winnipeg - Transcona

        //Saskatchewan
        POSTAL_TO_COORDS.put("S7K1J5", new double[]{52.1332, -106.6700}); // Saskatoon - Downtown
        POSTAL_TO_COORDS.put("S7N0W6", new double[]{52.1579, -106.6702}); // Saskatoon - University
        POSTAL_TO_COORDS.put("S4P3Y2", new double[]{50.4452, -104.6189}); // Regina - Downtown
        POSTAL_TO_COORDS.put("S4S0A2", new double[]{50.4113, -104.6189}); // Regina - South

        //Nova Scotia
        POSTAL_TO_COORDS.put("B3H4R2", new double[]{44.6488, -63.5752}); // Halifax - Downtown
        POSTAL_TO_COORDS.put("B3J3R7", new double[]{44.6476, -63.5728}); // Halifax - North End
        POSTAL_TO_COORDS.put("B3K5X5", new double[]{44.6820, -63.5752}); // Halifax - Clayton Park
        POSTAL_TO_COORDS.put("B3L4P1", new double[]{44.6951, -63.6281}); // Halifax - Fairview

        //New Brunswick
        POSTAL_TO_COORDS.put("E1C1G6", new double[]{46.0878, -64.7782}); // Moncton
        POSTAL_TO_COORDS.put("E3B5H1", new double[]{45.9636, -66.6431}); // Fredericton

        //Newfoundland and Labrador
        POSTAL_TO_COORDS.put("A1C5M3", new double[]{47.5615, -52.7126}); // St. John's - Downtown
        POSTAL_TO_COORDS.put("A1A1A1", new double[]{47.5675, -52.7371}); // St. John's

        //Prince Edward Island
        POSTAL_TO_COORDS.put("C1A4P3", new double[]{46.2382, -63.1311}); // Charlottetown

        //US ZIP CODES
        //Major US cities - Northeast
        ZIP_TO_COORDS.put("10001", new double[]{40.7508, -73.9973}); // New York, NY - Midtown
        ZIP_TO_COORDS.put("10002", new double[]{40.7156, -73.9862}); // New York, NY - Lower East Side
        ZIP_TO_COORDS.put("10003", new double[]{40.7310, -73.9897}); // New York, NY - East Village
        ZIP_TO_COORDS.put("10010", new double[]{40.7393, -73.9813}); // New York, NY - Gramercy
        ZIP_TO_COORDS.put("10011", new double[]{40.7406, -74.0006}); // New York, NY - Chelsea
        ZIP_TO_COORDS.put("10012", new double[]{40.7256, -73.9983}); // New York, NY - SoHo
        ZIP_TO_COORDS.put("10013", new double[]{40.7201, -74.0059}); // New York, NY - Tribeca
        ZIP_TO_COORDS.put("10014", new double[]{40.7342, -74.0065}); // New York, NY - West Village
        ZIP_TO_COORDS.put("10016", new double[]{40.7452, -73.9769}); // New York, NY - Murray Hill
        ZIP_TO_COORDS.put("10019", new double[]{40.7656, -73.9863}); // New York, NY - Midtown West
        ZIP_TO_COORDS.put("10021", new double[]{40.7685, -73.9595}); // New York, NY - Upper East Side
        ZIP_TO_COORDS.put("10022", new double[]{40.7577, -73.9691}); // New York, NY - Midtown East
        ZIP_TO_COORDS.put("10023", new double[]{40.7765, -73.9820}); // New York, NY - Upper West Side
        ZIP_TO_COORDS.put("10024", new double[]{40.7917, -73.9753}); // New York, NY - Upper West Side
        ZIP_TO_COORDS.put("10025", new double[]{40.7984, -73.9656}); // New York, NY - Upper West Side
        ZIP_TO_COORDS.put("10036", new double[]{40.7590, -73.9908}); // New York, NY - Times Square
        ZIP_TO_COORDS.put("11201", new double[]{40.6946, -73.9898}); // Brooklyn, NY - Brooklyn Heights
        ZIP_TO_COORDS.put("11211", new double[]{40.7093, -73.9536}); // Brooklyn, NY - Williamsburg
        ZIP_TO_COORDS.put("02108", new double[]{42.3584, -71.0600}); // Boston, MA - Beacon Hill
        ZIP_TO_COORDS.put("02109", new double[]{42.3601, -71.0542}); // Boston, MA - North End
        ZIP_TO_COORDS.put("02110", new double[]{42.3588, -71.0538}); // Boston, MA - Financial District
        ZIP_TO_COORDS.put("02115", new double[]{42.3426, -71.0978}); // Boston, MA - Longwood
        ZIP_TO_COORDS.put("02116", new double[]{42.3503, -71.0773}); // Boston, MA - Back Bay
        ZIP_TO_COORDS.put("02118", new double[]{42.3392, -71.0705}); // Boston, MA - South End
        ZIP_TO_COORDS.put("02119", new double[]{42.3240, -71.0848}); // Boston, MA - Roxbury
        ZIP_TO_COORDS.put("02120", new double[]{42.3294, -71.0988}); // Boston, MA - Mission Hill
        ZIP_TO_COORDS.put("02139", new double[]{42.3646, -71.1028}); // Cambridge, MA - MIT
        ZIP_TO_COORDS.put("19102", new double[]{39.9523, -75.1638}); // Philadelphia, PA - Center City
        ZIP_TO_COORDS.put("19103", new double[]{39.9535, -75.1670}); // Philadelphia, PA - Center City West
        ZIP_TO_COORDS.put("19106", new double[]{39.9501, -75.1497}); // Philadelphia, PA - Old City
        ZIP_TO_COORDS.put("19107", new double[]{39.9496, -75.1627}); // Philadelphia, PA - Washington Square

        //Major US cities - Midwest
        ZIP_TO_COORDS.put("60601", new double[]{41.8857, -87.6185}); // Chicago, IL - The Loop
        ZIP_TO_COORDS.put("60602", new double[]{41.8825, -87.6306}); // Chicago, IL - Loop
        ZIP_TO_COORDS.put("60603", new double[]{41.8795, -87.6294}); // Chicago, IL - Grant Park
        ZIP_TO_COORDS.put("60604", new double[]{41.8769, -87.6298}); // Chicago, IL - South Loop
        ZIP_TO_COORDS.put("60605", new double[]{41.8691, -87.6198}); // Chicago, IL - Bronzeville
        ZIP_TO_COORDS.put("60606", new double[]{41.8832, -87.6385}); // Chicago, IL - West Loop
        ZIP_TO_COORDS.put("60607", new double[]{41.8730, -87.6527}); // Chicago, IL - University Village
        ZIP_TO_COORDS.put("60610", new double[]{41.9018, -87.6341}); // Chicago, IL - Gold Coast
        ZIP_TO_COORDS.put("60611", new double[]{41.8931, -87.6238}); // Chicago, IL - River North
        ZIP_TO_COORDS.put("60614", new double[]{41.9214, -87.6527}); // Chicago, IL - Lincoln Park
        ZIP_TO_COORDS.put("48201", new double[]{42.3504, -83.1270}); // Detroit, MI - Midtown
        ZIP_TO_COORDS.put("48202", new double[]{42.3694, -83.0859}); // Detroit, MI - New Center
        ZIP_TO_COORDS.put("55401", new double[]{44.9794, -93.2656}); // Minneapolis, MN - Downtown
        ZIP_TO_COORDS.put("55402", new double[]{44.9778, -93.2728}); // Minneapolis, MN - Downtown West
        ZIP_TO_COORDS.put("43201", new double[]{40.0094, -83.0133}); // Columbus, OH - Clintonville

        //Major US cities - South
        ZIP_TO_COORDS.put("20001", new double[]{38.9072, -77.0134}); // Washington, DC - Capitol Hill
        ZIP_TO_COORDS.put("20002", new double[]{38.8993, -77.0006}); // Washington, DC - Capitol Hill North
        ZIP_TO_COORDS.put("20003", new double[]{38.8845, -77.0026}); // Washington, DC - Capitol Hill SE
        ZIP_TO_COORDS.put("20004", new double[]{38.8961, -77.0239}); // Washington, DC - Downtown
        ZIP_TO_COORDS.put("20005", new double[]{38.9063, -77.0366}); // Washington, DC - Mount Vernon Square
        ZIP_TO_COORDS.put("20006", new double[]{38.8982, -77.0391}); // Washington, DC - Penn Quarter
        ZIP_TO_COORDS.put("20007", new double[]{38.9144, -77.0723}); // Washington, DC - Georgetown
        ZIP_TO_COORDS.put("20008", new double[]{38.9359, -77.0560}); // Washington, DC - Cleveland Park
        ZIP_TO_COORDS.put("20009", new double[]{38.9179, -77.0365}); // Washington, DC - U Street
        ZIP_TO_COORDS.put("20010", new double[]{38.9298, -77.0321}); // Washington, DC - Columbia Heights
        ZIP_TO_COORDS.put("30301", new double[]{33.7490, -84.3880}); // Atlanta, GA - Downtown
        ZIP_TO_COORDS.put("30302", new double[]{33.7633, -84.3881}); // Atlanta, GA - Georgia Tech
        ZIP_TO_COORDS.put("33101", new double[]{25.7774, -80.1900}); // Miami, FL - Downtown
        ZIP_TO_COORDS.put("33109", new double[]{25.7832, -80.1301}); // Miami Beach, FL
        ZIP_TO_COORDS.put("33125", new double[]{25.7870, -80.2346}); // Miami, FL - Flagami
        ZIP_TO_COORDS.put("33126", new double[]{25.7730, -80.2955}); // Miami, FL - Westchester
        ZIP_TO_COORDS.put("75201", new double[]{32.7831, -96.7982}); // Dallas, TX - Downtown
        ZIP_TO_COORDS.put("75202", new double[]{32.7809, -96.7972}); // Dallas, TX - Downtown East
        ZIP_TO_COORDS.put("77001", new double[]{29.7589, -95.3677}); // Houston, TX - Downtown
        ZIP_TO_COORDS.put("77002", new double[]{29.7604, -95.3698}); // Houston, TX - Downtown
        ZIP_TO_COORDS.put("77003", new double[]{29.7520, -95.3527}); // Houston, TX - EaDo
        ZIP_TO_COORDS.put("78701", new double[]{30.2711, -97.7437}); // Austin, TX - Downtown
        ZIP_TO_COORDS.put("78702", new double[]{30.2605, -97.7211}); // Austin, TX - East Austin
        ZIP_TO_COORDS.put("78703", new double[]{30.2808, -97.7645}); // Austin, TX - West Austin
        ZIP_TO_COORDS.put("78704", new double[]{30.2447, -97.7633}); // Austin, TX - South Austin
        ZIP_TO_COORDS.put("78705", new double[]{30.2904, -97.7431}); // Austin, TX - University

        //Major US cities - West
        ZIP_TO_COORDS.put("90001", new double[]{33.9731, -118.2479}); // Los Angeles, CA - South LA
        ZIP_TO_COORDS.put("90002", new double[]{33.9489, -118.2467}); // Los Angeles, CA - Watts
        ZIP_TO_COORDS.put("90003", new double[]{33.9625, -118.2728}); // Los Angeles, CA - South LA
        ZIP_TO_COORDS.put("90004", new double[]{34.0766, -118.3090}); // Los Angeles, CA - Koreatown
        ZIP_TO_COORDS.put("90005", new double[]{34.0598, -118.3089}); // Los Angeles, CA - Wilshire
        ZIP_TO_COORDS.put("90006", new double[]{34.0487, -118.2926}); // Los Angeles, CA - MacArthur Park
        ZIP_TO_COORDS.put("90007", new double[]{34.0251, -118.2851}); // Los Angeles, CA - USC
        ZIP_TO_COORDS.put("90008", new double[]{34.0115, -118.3410}); // Los Angeles, CA - Baldwin Hills
        ZIP_TO_COORDS.put("90012", new double[]{34.0655, -118.2386}); // Los Angeles, CA - Chinatown
        ZIP_TO_COORDS.put("90013", new double[]{34.0405, -118.2468}); // Los Angeles, CA - Downtown
        ZIP_TO_COORDS.put("90014", new double[]{34.0436, -118.2542}); // Los Angeles, CA - Downtown
        ZIP_TO_COORDS.put("90015", new double[]{34.0389, -118.2665}); // Los Angeles, CA - Pico-Union
        ZIP_TO_COORDS.put("90016", new double[]{34.0327, -118.3524}); // Los Angeles, CA - Crenshaw
        ZIP_TO_COORDS.put("90017", new double[]{34.0522, -118.2571}); // Los Angeles, CA - Westlake
        ZIP_TO_COORDS.put("90018", new double[]{34.0279, -118.3089}); // Los Angeles, CA - Arlington Heights
        ZIP_TO_COORDS.put("90019", new double[]{34.0490, -118.3430}); // Los Angeles, CA - Mid-Wilshire
        ZIP_TO_COORDS.put("90020", new double[]{34.0670, -118.3090}); // Los Angeles, CA - Koreatown
        ZIP_TO_COORDS.put("90021", new double[]{34.0344, -118.2353}); // Los Angeles, CA - Fashion District
        ZIP_TO_COORDS.put("90024", new double[]{34.0628, -118.4426}); // Los Angeles, CA - Westwood
        ZIP_TO_COORDS.put("90025", new double[]{34.0426, -118.4512}); // Los Angeles, CA - West LA
        ZIP_TO_COORDS.put("90026", new double[]{34.0775, -118.2654}); // Los Angeles, CA - Echo Park
        ZIP_TO_COORDS.put("90027", new double[]{34.1047, -118.2984}); // Los Angeles, CA - Los Feliz
        ZIP_TO_COORDS.put("90028", new double[]{34.0990, -118.3268}); // Los Angeles, CA - Hollywood
        ZIP_TO_COORDS.put("90029", new double[]{34.0896, -118.2948}); // Los Angeles, CA - East Hollywood
        ZIP_TO_COORDS.put("90036", new double[]{34.0647, -118.3531}); // Los Angeles, CA - Miracle Mile
        ZIP_TO_COORDS.put("90046", new double[]{34.1079, -118.3615}); // Los Angeles, CA - Hollywood Hills
        ZIP_TO_COORDS.put("94102", new double[]{37.7796, -122.4193}); // San Francisco, CA - Tenderloin
        ZIP_TO_COORDS.put("94103", new double[]{37.7726, -122.4099}); // San Francisco, CA - SoMa
        ZIP_TO_COORDS.put("94104", new double[]{37.7913, -122.4021}); // San Francisco, CA - Financial District
        ZIP_TO_COORDS.put("94105", new double[]{37.7875, -122.3900}); // San Francisco, CA - SoMa East
        ZIP_TO_COORDS.put("94107", new double[]{37.7619, -122.3992}); // San Francisco, CA - Potrero Hill
        ZIP_TO_COORDS.put("94108", new double[]{37.7927, -122.4077}); // San Francisco, CA - Chinatown
        ZIP_TO_COORDS.put("94109", new double[]{37.7915, -122.4204}); // San Francisco, CA - Nob Hill
        ZIP_TO_COORDS.put("94110", new double[]{37.7487, -122.4157}); // San Francisco, CA - Mission
        ZIP_TO_COORDS.put("94111", new double[]{37.7978, -122.3993}); // San Francisco, CA - Financial District
        ZIP_TO_COORDS.put("94112", new double[]{37.7213, -122.4424}); // San Francisco, CA - Outer Mission
        ZIP_TO_COORDS.put("94114", new double[]{37.7583, -122.4348}); // San Francisco, CA - Castro
        ZIP_TO_COORDS.put("94115", new double[]{37.7864, -122.4364}); // San Francisco, CA - Western Addition
        ZIP_TO_COORDS.put("94116", new double[]{37.7432, -122.4862}); // San Francisco, CA - Parkside
        ZIP_TO_COORDS.put("94117", new double[]{37.7706, -122.4409}); // San Francisco, CA - Haight-Ashbury
        ZIP_TO_COORDS.put("94118", new double[]{37.7813, -122.4668}); // San Francisco, CA - Inner Richmond
        ZIP_TO_COORDS.put("94121", new double[]{37.7770, -122.4928}); // San Francisco, CA - Outer Richmond
        ZIP_TO_COORDS.put("94122", new double[]{37.7594, -122.4862}); // San Francisco, CA - Sunset
        ZIP_TO_COORDS.put("94123", new double[]{37.8008, -122.4381}); // San Francisco, CA - Marina
        ZIP_TO_COORDS.put("94124", new double[]{37.7334, -122.3933}); // San Francisco, CA - Bayview
        ZIP_TO_COORDS.put("94131", new double[]{37.7405, -122.4367}); // San Francisco, CA - Glen Park
        ZIP_TO_COORDS.put("94133", new double[]{37.8008, -122.4102}); // San Francisco, CA - North Beach
        ZIP_TO_COORDS.put("94134", new double[]{37.7189, -122.4150}); // San Francisco, CA - Visitacion Valley
        ZIP_TO_COORDS.put("98101", new double[]{47.6101, -122.3341}); // Seattle, WA - Downtown
        ZIP_TO_COORDS.put("98102", new double[]{47.6291, -122.3231}); // Seattle, WA - Capitol Hill
        ZIP_TO_COORDS.put("98103", new double[]{47.6696, -122.3419}); // Seattle, WA - Fremont
        ZIP_TO_COORDS.put("98104", new double[]{47.6038, -122.3301}); // Seattle, WA - Pioneer Square
        ZIP_TO_COORDS.put("98105", new double[]{47.6654, -122.3032}); // Seattle, WA - University District
        ZIP_TO_COORDS.put("98106", new double[]{47.5293, -122.3540}); // Seattle, WA - White Center
        ZIP_TO_COORDS.put("97201", new double[]{45.5155, -122.6937}); // Portland, OR - Downtown
        ZIP_TO_COORDS.put("97202", new double[]{45.4842, -122.6434}); // Portland, OR - Brooklyn
        ZIP_TO_COORDS.put("97203", new double[]{45.5952, -122.7371}); // Portland, OR - St. Johns
        ZIP_TO_COORDS.put("97204", new double[]{45.5183, -122.6756}); // Portland, OR - Downtown
        ZIP_TO_COORDS.put("97205", new double[]{45.5253, -122.6964}); // Portland, OR - West Hills
        ZIP_TO_COORDS.put("97209", new double[]{45.5279, -122.6814}); // Portland, OR - Pearl District
        ZIP_TO_COORDS.put("97210", new double[]{45.5329, -122.7075}); // Portland, OR - Northwest District
        ZIP_TO_COORDS.put("97211", new double[]{45.5619, -122.6458}); // Portland, OR - Concordia
        ZIP_TO_COORDS.put("97212", new double[]{45.5420, -122.6453}); // Portland, OR - Irvington
        ZIP_TO_COORDS.put("97214", new double[]{45.5157, -122.6445}); // Portland, OR - Hawthorne
        ZIP_TO_COORDS.put("97215", new double[]{45.5110, -122.6054}); // Portland, OR - Mt. Tabor
        ZIP_TO_COORDS.put("85001", new double[]{33.4484, -112.0740}); // Phoenix, AZ - Downtown
        ZIP_TO_COORDS.put("85004", new double[]{33.4484, -112.0740}); // Phoenix, AZ - Downtown
        ZIP_TO_COORDS.put("80202", new double[]{39.7539, -104.9910}); // Denver, CO - LoDo
        ZIP_TO_COORDS.put("80203", new double[]{39.7392, -104.9848}); // Denver, CO - Capitol Hill
        ZIP_TO_COORDS.put("80204", new double[]{39.7484, -105.0048}); // Denver, CO - Highlands
        ZIP_TO_COORDS.put("80205", new double[]{39.7627, -104.9756}); // Denver, CO - Five Points
        ZIP_TO_COORDS.put("80206", new double[]{39.7294, -104.9536}); // Denver, CO - Cheesman Park
        ZIP_TO_COORDS.put("80209", new double[]{39.7042, -104.9693}); // Denver, CO - Cherry Creek
        ZIP_TO_COORDS.put("80211", new double[]{39.7671, -105.0020}); // Denver, CO - Berkeley
        ZIP_TO_COORDS.put("80218", new double[]{39.7320, -104.9710}); // Denver, CO - Congress Park
        ZIP_TO_COORDS.put("80220", new double[]{39.7307, -104.9075}); // Denver, CO - Montclair
    }

    /**
     *Gets the latitude and longitude for a given ZIP/postal code.
     *Supports both US ZIP codes (12345) and Canadian postal codes (A1A 1A1 or A1A1A1).
     *
     *@param code ZIP or postal code
     *@return double array {latitude, longitude} or null if not found
     */
    public static double[] getCoordinates(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String normalized = normalizePostalCode(code);

        //Try US ZIP first
        double[] coords = ZIP_TO_COORDS.get(normalized);
        if (coords != null) {
            return coords;
        }

        //Try Canadian postal code
        return POSTAL_TO_COORDS.get(normalized);
    }

    /**
     *Validates if a ZIP/postal code is properly formatted and exists in our database.
     *Supports both US ZIP codes (12345) and Canadian postal codes (A1A 1A1 or A1A1A1).
     *
     *@param code ZIP or postal code to validate
     *@return true if code is valid format and exists in database
     */
    public static boolean isValidZip(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        String trimmed = code.trim();
        String normalized = normalizePostalCode(trimmed);

        // Check US ZIP format: exactly 5 digits
        if (trimmed.matches("^\\d{5}$")) {
            return ZIP_TO_COORDS.containsKey(normalized);
        }

        // Check Canadian postal code format: A1A 1A1 or A1A1A1
        if (trimmed.matches("^[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d$")) {
            return POSTAL_TO_COORDS.containsKey(normalized);
        }

        return false;
    }

    /**
     *Normalizes a postal code for lookup.
     *US ZIP: returns as-is (already numeric)
     *Canadian: removes spaces and converts to uppercase
     *
     *@param code ZIP or postal code
     *@return normalized code for lookup
     */
    private static String normalizePostalCode(String code) {
        if (code == null) {
            return null;
        }
        // Remove spaces and convert to uppercase
        return code.trim().replace(" ", "").toUpperCase();
    }

    /**
     *Returns the total number of ZIP/postal codes in the database.
     *Useful for debugging/testing.
     *
     *@return number of codes available
     */
    public static int getZipCodeCount() {
        return ZIP_TO_COORDS.size() + POSTAL_TO_COORDS.size();
    }

    /**
     *Formats a postal code for display.
     *US ZIP: returns as-is
     *Canadian: adds space in middle (A1A1A1 -> A1A 1A1)
     *
     *@param code ZIP or postal code
     *@return formatted code for display
     */
    public static String formatPostalCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }

        String normalized = normalizePostalCode(code);

        //If it's a Canadian postal code (6 alphanumeric), add space
        if (normalized.matches("^[A-Z]\\d[A-Z]\\d[A-Z]\\d$")) {
            return normalized.substring(0, 3) + " " + normalized.substring(3);
        }
        return code.trim();
    }
}
