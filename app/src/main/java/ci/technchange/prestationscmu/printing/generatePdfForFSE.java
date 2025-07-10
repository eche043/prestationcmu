package ci.technchange.prestationscmu.printing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.File;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.core.GlobalClass;

//public class generatePdfForFSE extends Activity {
public class generatePdfForFSE {
    File root;
    Bitmap pageImage;
    private TextView statusTextView;
    private ProgressBar progressBar;
    String donneeToPrint;

    public generatePdfForFSE(String jsonData) {
        donneeToPrint = jsonData;
        genereEtPrintFSE(donneeToPrint);
    }

    /*
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_generate_pdf);
            statusTextView = findViewById(R.id.statusTextView);
            progressBar = findViewById(R.id.progressBar);
        }

        @Override
        protected void onStart() {
            super.onStart();
            setup();
        }
    */
   // private void setup() {
    public void genereEtPrintFSE(String jsonData) {
        // Enable Android asset loading
        //PDFBoxResourceLoader.init(getApplicationContext());
        PDFBoxResourceLoader.init(GlobalClass.getInstance().getApplicationContext());
        // Find the root of the external storage.
        //root = getApplicationContext().getCacheDir();
        root = GlobalClass.getInstance().getApplicationContext().getCacheDir();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //String jsonData = getIntent().getStringExtra("json_data");

        if(jsonData==null){
            //statusTextView.setText("Démarrage de l'application. Pas de données envoyées");
            //progressBar.setVisibility(ProgressBar.GONE);
            //Toast.makeText(generatePdfForFSE.this, "Aucune données transmise", Toast.LENGTH_SHORT).show();
            Toast.makeText(GlobalClass.getInstance().getApplicationContext(), "Aucune données transmise", Toast.LENGTH_SHORT).show();
        }else {
            //progressBar.setVisibility(ProgressBar.VISIBLE);
            //statusTextView.setText("Génération du PDF en cours...");
            Toast.makeText(GlobalClass.getInstance().getApplicationContext(), "1- Génération du PDF en cours...", Toast.LENGTH_SHORT).show();
            try {
                // Créez le PDF à partir des données JSON
                File pdfFile = createPdf(jsonData);
                //statusTextView.setText(statusTextView.getText()+"\nPDF généré. Envoi du PDF en cours...");
                Toast.makeText(GlobalClass.getInstance().getApplicationContext(), "2- PDF généré. Envoi du PDF en cours...", Toast.LENGTH_SHORT).show();
                renderFile(pdfFile);
                // Envoyer le fichier PDF à l'URL spécifiée
                String url = "http://10.10.0.1:8000";
                sendPdfToServer(pdfFile, url);
                //statusTextView.setText( statusTextView.getText()+"\nPDF généré et envoyé avec succès!");
                Toast.makeText(GlobalClass.getInstance().getApplicationContext(), "3- PDF envoyé avec succès!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                //statusTextView.setText(statusTextView.getText()+"\nErreur lors de la génération ou de l'envoi du PDF");
                Toast.makeText(GlobalClass.getInstance().getApplicationContext(), "Erreur lors de la génération ou de l'envoi du PDF", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    public void addTextAtPosition(PDPageContentStream contentStream, float x, float y, String text, int fontSize) throws Exception {
        contentStream.beginText();
        //contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        System.out.println(text+" aux coordonnées: " + x + " -- " + y);
    }

    public PointF convertCmToPoints(float xCm, float yCm) {
        float xPoints = xCm * 28.35f;
        float yPoints = (29.7f - yCm) * 28.35f;
        System.out.println("Coordonnées: " + xPoints + " -- " + yPoints);
        return new PointF(xPoints, yPoints);
    }

    /**
     * Creates a new PDF from scratch and saves it to a file
     */
    public File createPdf(String jsonData) throws IOException{
        Gson gson = new Gson();
        //Map<String, Object> data = gson.fromJson(jsonData, Map.class);
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> data = gson.fromJson(jsonData, mapType);

        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        // Create a new font object selecting one of the PDF base fonts
        PDFont font = PDType1Font.HELVETICA;
        page.setCropBox(new PDRectangle(0.5f, 29.7f-2.9f, 580, 759)); // Zone d'impression
        page.setMediaBox(new PDRectangle(0, 0, 595, 842)); // Format du papier
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        int defaultFontSize = 11;
        PointF position;
        try {
            // Define a content stream for adding to the PDF
            //contentStream = new PDPageContentStream(document, page);

            position =  convertCmToPoints(0.5f, 3.8f); // Coordonnées en cm depuis la bordure supérieure gauche
            addTextAtPosition(contentStream, position.x, position.y, data.get("dateSoins").toString(), defaultFontSize);
            position= convertCmToPoints(5.2f, 3.8f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("OGD").toString(), 12);
            position = convertCmToPoints(0.5f, 4.7f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numFsInitiale").toString(), defaultFontSize);

            position =  convertCmToPoints(5.2f, 4.7f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numTransaction").toString(), defaultFontSize);

            position =  convertCmToPoints(0.5f, 5.6f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numEntentePrealable").toString(), defaultFontSize);

            position =  convertCmToPoints(5.2f, 5.6f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numEntentePrealableAC").toString(), defaultFontSize);

            position =  convertCmToPoints(10.5f, 3.8f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("nomPrenomsAssure").toString(), defaultFontSize);

            position =  convertCmToPoints(10.5f, 4.7f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numSecu").toString(), defaultFontSize);

            position =  convertCmToPoints(14.3f, 4.7f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("dateNaissance").toString(), defaultFontSize);
            PointF positionGenre;
            positionGenre =(data.get("genre").toString()=="M")?convertCmToPoints(19.0f, 4.7f):convertCmToPoints(19.7f, 4.7f);
            addTextAtPosition(contentStream, positionGenre.x, positionGenre.y, "x", defaultFontSize);

            position =  convertCmToPoints(10.5f, 5.6f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("numAssureAC").toString(), defaultFontSize);

            position =  convertCmToPoints(13.0f, 5.6f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("codeAC").toString(), defaultFontSize);

            position =  convertCmToPoints(15.2f, 5.6f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("nomAC").toString(), defaultFontSize);

            position =  convertCmToPoints(0.5f, 7.4f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("codeEtablissement").toString(), defaultFontSize);

            position =  convertCmToPoints(5.3f, 7.4f);
            addTextAtPosition(contentStream, position.x, position.y, data.get("nomEtablissement").toString(), defaultFontSize);
            // ****************** FIN DE ZONE ENTETE DE LA FSE ********************************//

            //*************** Specificicité de chaque feuille de soins ********************//

            if(data.get("typeFSE").toString()!="dentaire"){
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("cocheCMR").toString(), defaultFontSize-2);
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("cocheUrgence").toString(), defaultFontSize-2);
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("cocheEloignement").toString(), defaultFontSize-2);
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("cochereference").toString(), defaultFontSize-2);
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("cocheAutre").toString(), defaultFontSize-2);
                position =  convertCmToPoints(2.0f, 2.0f);
                addTextAtPosition(contentStream, position.x, position.y, data.get("precisionEtablissementAccueil").toString(), defaultFontSize);
            }

            PointF positionCodeProfessionnelSante = convertCmToPoints(0.5f, 9.1f);
            addTextAtPosition(contentStream, positionCodeProfessionnelSante.x, positionCodeProfessionnelSante.y,  data.get("codeProfessionnelSante").toString(), defaultFontSize);

            PointF positionNomProfessionnelSante = convertCmToPoints(3.7f, 9.1f);
            addTextAtPosition(contentStream, positionNomProfessionnelSante.x, positionNomProfessionnelSante.y,  data.get("nomProfessionnelSante").toString(), defaultFontSize);

            PointF positionSpeciaProfessionnelSante = convertCmToPoints(12.6f, 9.1f);
            addTextAtPosition(contentStream, positionSpeciaProfessionnelSante.x, positionSpeciaProfessionnelSante.y,  data.get("specialiteProfessionnelSante").toString(), defaultFontSize);

            PointF positionInfoComp_Maternite = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_Maternite.x, positionInfoComp_Maternite.y,  data.get("infoComp_Maternite").toString(), defaultFontSize);

            PointF positionInfoComp_AVP = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_AVP.x, positionInfoComp_AVP.y,  data.get("infoComp_AVP").toString(), defaultFontSize);

            PointF positionInfoComp_ATMP = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_ATMP.x, positionInfoComp_ATMP.y,  data.get("infoComp_ATMP").toString(), defaultFontSize);

            PointF positionInfoComp_AUTRE = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_AUTRE.x, positionInfoComp_AUTRE.y,  data.get("infoComp_AUTRE").toString(), defaultFontSize);

            PointF positionInfoComp_PROGSPECIAL = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_PROGSPECIAL.x, positionInfoComp_PROGSPECIAL.y,  data.get("infoComp_PROGSPECIAL").toString(), defaultFontSize);

            PointF positionInfoComp_CODE = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_CODE.x, positionInfoComp_CODE.y,  data.get("infoComp_CODE").toString(), defaultFontSize);

            PointF positionInfoComp_IMMVEH = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_IMMVEH.x, positionInfoComp_IMMVEH.y,  data.get("infoComp_IMMVEH").toString(), defaultFontSize);

            PointF positionInfoComp_Observation = convertCmToPoints(2.0f, 2.0f);
            addTextAtPosition(contentStream, positionInfoComp_Observation.x, positionInfoComp_Observation.y,  data.get("infoComp_Observation").toString(), defaultFontSize);

            PointF positionCodeAffection1 = convertCmToPoints(3.4f, 13.6f);
            addTextAtPosition(contentStream, positionCodeAffection1.x, positionCodeAffection1.y,  data.get("codeAffection1").toString(), defaultFontSize);

            PointF positionCodeAffection2 = convertCmToPoints(14.9f, 13.6f);
            addTextAtPosition(contentStream, positionCodeAffection2.x, positionCodeAffection2.y,  data.get("codeAffection2").toString(), defaultFontSize);

            List<Map<String, Object>> listePrestation = (List<Map<String, Object>>) data.get("prestations");

            float pasPrestation = 0.9f;
            int i=0;
            for (Map<String, Object> unePrestation : listePrestation) {

                PointF positioncodeActe = convertCmToPoints(0.6f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positioncodeActe.x, positioncodeActe.y,  unePrestation.get("codeActe").toString(), defaultFontSize);

                PointF positiondesignation = convertCmToPoints(2.5f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positiondesignation.x, positiondesignation.y,  unePrestation.get("designation").toString(), defaultFontSize);

                PointF positiondateDebut = convertCmToPoints(8.0f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positiondateDebut.x, positiondateDebut.y,  unePrestation.get("dateDebut").toString(), defaultFontSize);

                PointF positiondateFin = convertCmToPoints(9.8f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positiondateFin.x, positiondateFin.y,  unePrestation.get("dateFin").toString(), defaultFontSize);

                // PointF positionnumDent = convertCmToPoints(2.0f, 15.1f+pasPrestation*i);
                // addTextAtPosition(contentStream, positionnumDent.x, positionnumDent.y,  unePrestation.get("numDent").toString(), defaultFontSize);

                PointF positionquantite = convertCmToPoints(11.6f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positionquantite.x, positionquantite.y,  unePrestation.get("quantite").toString(), defaultFontSize);

                PointF positionmontant = convertCmToPoints(12.7f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positionmontant.x, positionmontant.y,  unePrestation.get("montant").toString(), defaultFontSize);

                PointF positionpartCmu = convertCmToPoints(15.1f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positionpartCmu.x, positionpartCmu.y,  unePrestation.get("partCmu").toString(), defaultFontSize);

                PointF positionpartAC = convertCmToPoints(17.0f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positionpartAC.x, positionpartAC.y,  unePrestation.get("partAC").toString(), defaultFontSize);

                PointF positionpartAssure = convertCmToPoints(18.8f, 15.1f+pasPrestation*i);
                addTextAtPosition(contentStream, positionpartAssure.x, positionpartAssure.y,  unePrestation.get("partAssure").toString(), defaultFontSize);
                i++;
            }

            // Draw a green rectangle
            //contentStream.addRect(5, 500, 100, 100);
            //contentStream.setNonStrokingColor(0, 255, 125);
            //contentStream.fill();


        } catch (Exception e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF", e);
        }
        // Make sure that the content stream is closed:
        contentStream.close();

        // Save the final pdf document to a file
        String directoryPath = root.getAbsolutePath();
        File file = new File(directoryPath, "Created.pdf");

        document.save(file);
        document.close();
        //statusTextView.setText("Successfully wrote PDF to " + directoryPath+"/Created.pdf");
        return file;
    }

    public void sendPdfToServer(File pdfFile, String urlString) throws IOException {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        FileInputStream fileInputStream = new FileInputStream(pdfFile);

        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();

        // Configuration de la connexion
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("ENCTYPE", "multipart/form-data");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        connection.setRequestProperty("data", pdfFile.getName());

        outputStream = new DataOutputStream(connection.getOutputStream());

        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"data\";filename=\"" + pdfFile.getName() + "\"" + lineEnd);
        outputStream.writeBytes(lineEnd);

        // Écrire le fichier PDF dans le flux de sortie
        int bytesAvailable = fileInputStream.available();
        int maxBufferSize = 1 * 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        outputStream.flush();
        outputStream.close();
        fileInputStream.close();

        // Vérification de la réponse du serveur
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Échec de l'envoi du fichier PDF : " + responseCode);
        }
    }

    public void renderFile(File file) {
        // Render the page and save it to an image file
        try {
            // Load in an already created PDF
            PDDocument document = PDDocument.load(file);
            // Create a renderer for the document
            PDFRenderer renderer = new PDFRenderer(document);
            // Render the image to an RGB Bitmap
            pageImage = renderer.renderImage(0, 1, ImageType.RGB);

            // Save the render result to an image
            String path = root.getAbsolutePath() + "/render.jpg";
            File renderFile = new File(path);
            FileOutputStream fileOut = new FileOutputStream(renderFile);
            pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
            fileOut.close();
            //statusTextView.setText(statusTextView.getText()+"\nSuccessfully rendered image to " + path);
            // Optional: display the render result on screen
            //displayRenderedImage();
        }
        catch (IOException e)
        {
            Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e);
        }
    }


    /**
     * Helper method for drawing the result of renderFile() on screen
     */
    /*private void displayRenderedImage() {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.renderedImageView);
                        imageView.setImageBitmap(pageImage);
                    }
                });
            }
        }.start();
    } */


}
