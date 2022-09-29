import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;

import static io.restassured.RestAssured.given;

public class Trendyol {

    static String token;

    public static void main(String[] args) {
        System.out.println("Trendyol'a Hoşgeldiniz!\n");
        login();
        menu();

    }

    public static void login() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Email adresinizi giriniz: ");
        String email = scanner.nextLine();
        System.out.print("Şifrenizi giriniz: ");
        String password = scanner.nextLine();

        Response loginRes =
                given().
                        header("culture", "tr-TR").
                        header("storefront-id", "1").
                        header("application-id", "1").
                        contentType(ContentType.JSON).
                        body(String.format("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }""", email, password)).
                when().
                        post("https://auth.trendyol.com/login").
                then().
                        statusCode(200).
                extract()
                        .response();

        System.out.println("Giriş Başarılı!");
        token = loginRes.cookie("access_token");
    }

    public static void menu() {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("""
                           
                           Menü:
                           1) Arama
                           2) Sepetim
                           3) Kayıtlı Adreslerim
                           
                           Seçiniz:\040""");
            int menuSecim = scanner.nextInt();

            switch (menuSecim) {
                case 1 -> search();
                case 2 -> showBasket();
                case 3 -> getAddresses();
            }
        }
    }

    public static void search() {

            Scanner scanner = new Scanner(System.in);
            System.out.print("\nArama: ");

            String arama = scanner.nextLine();
        while (true) {
            String searchResult =
                    given().
                            header("authorization", token).
                    when().
                            get("https://public.trendyol.com/discovery-web-searchgw-service/v2/api/filter/sr?q=" + arama).
                    then().
//                            log().body().
                            statusCode(200).
                    extract()
                            .body().asString();


            JSONObject body = new JSONObject(searchResult);
            JSONArray products = new JSONArray(body.getJSONObject("result")
                    .getJSONArray("products"));

            System.out.printf("%-25s", "Ürün Fiyatı:");
            System.out.println("Ürün Adı:");

            for (int i = 0; i < products.length(); i++) {
                System.out.printf("%5s", (i + 1) + ") ");
                System.out.printf("%-20s", products.getJSONObject(i)
                        .getJSONObject("price").get("discountedPrice") + " TL");
                System.out.println(products.getJSONObject(i).get("name"));
            }

            System.out.print("""
                
                Sepete ürün eklemek için +
                Ana menüye geri dönmek için -
                Yeni arama yapmak için aramak istediğiniz kelimeyi giriniz:\s""");

            String aramaSecim = scanner.nextLine();

            if (aramaSecim.equals("+")) {
                System.out.print("Sepete eklemek istediğiniz ürünün numarasını giriniz: ");
                int urunSecim = scanner.nextInt();
                System.out.print("Kaç adet eklemek istiyorsunuz? ");
                int quantity = scanner.nextInt();
                addToBasket(products.getJSONObject(urunSecim - 1), quantity);
                break;
            } else if (aramaSecim.equals("-"))
                break;
            else
                arama = aramaSecim;
        }
    }

    public static void addToBasket(JSONObject product, int quantity) {
        String contentId = product.get("id").toString();
        String campaignId = product.getJSONArray("variants").getJSONObject(0).get("campaignId").toString();
        String listingId = product.getJSONArray("variants").getJSONObject(0).getString("listingId");
        String merchantId = product.getJSONArray("variants").getJSONObject(0).get("merchantId").toString();

            given().
                    header("authorization", token).
                    contentType(ContentType.JSON).
            body(String.format("""
                            {
                              "contentId": %s,
                              "campaignId": %s,
                              "listingId": "%s",
                              "merchantId": %s,
                              "quantity": %s,
                              "vasItems": []
                            }""", contentId, campaignId, listingId, merchantId, quantity)).
            when().
                    post("https://public-mdc.trendyol.com/discovery-web-checkout-service/api/basket/v2/add").
            then().
//                    log().body().
                    statusCode(200);

        System.out.println("Ürün sepete başarı ile eklendi!");
    }

    public static void showBasket() {
        String showBasket =
                given().
                        header("authorization", token).
                when().
                        get("https://public-mdc.trendyol.com/discovery-web-checkout-service/basket/fragment/sepet").
                then().
//                        log().body().
                        statusCode(200).
                extract()
                        .body().asString();

        System.out.println("\nSepetinizdeki Ürünler:\n");

        JSONObject body = new JSONObject(showBasket);
        JSONArray items = new JSONArray(body.getJSONObject("result")
                .getJSONObject("data").getJSONArray("items"));

        for (int i = 0; i < items.length(); i++) {
            System.out.println("Ürün adı: " + items.getJSONObject(i).get("name"));
            System.out.println("Fiyat: " + items.getJSONObject(i)
                    .getJSONObject("price").get("totalPrice") + " TL");
            System.out.println("Adet: " + items.getJSONObject(i).get("quantity"));
            System.out.println();
        }

        System.out.println("Sepet Toplam: " + body.getJSONObject("result")
                                           .getJSONObject("data")
                                           .getJSONObject("summary")
                                           .getJSONObject("prices").get("totalPrice") + " TL\n");

    }

    public static void getAddresses() {
        String addresses =
                given().
                        header("authorization", token).
                        when().
                        get("https://public-sdc.trendyol.com/discovery-web-accountgw-service/api/address/list/mask").
                then().
//                        log().body().
                        statusCode(200).
                extract()
                        .body().asString();

        JSONObject body = new JSONObject(addresses);
        JSONArray items = new JSONArray(body.getJSONArray("result"));

        System.out.print("\nKayıtlı Adresleriniz: \n");

        for (int i = 0; i < items.length(); i++) {
            System.out.println((i + 1) + ") " + items.getJSONObject(i).getString("addressLine"));
        }

    }
}
