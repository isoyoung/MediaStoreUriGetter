package isoyoung.com.mediastoreurigetter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_DIR_CODE = 1;

    private Uri mCurrentUri;

    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.saf_button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSaf();
            }
        });
    }

    private void showSaf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_DIR_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DIR_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            mCurrentUri = uri;


            String realFilePath = getRealFilePath(uri);
            if (realFilePath == null) {
                Log.d(TAG, "Not found real path.");
            } else {
                Log.d(TAG, realFilePath + "is found.");
            }
        }
    }


    // TODO: 2017/05/08 SAFでは使えない
//    private String getExif(String filePath) {
//
//        try {
//            ExifInterface ei = new ExifInterface(filePath);
//            String result = ei.getAttribute(ExifInterface.TAG_F_NUMBER);
//            return result;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // ここにきてはいけない
//        return null;
//    }

    private String getExifUsingUri(Uri treeUri) {

        DocumentFile[] files = DocumentFile.fromTreeUri(this, treeUri).listFiles();

        String fNumber = null;

        for (DocumentFile file : files) {
            Uri childUri = file.getUri();
            try {
                InputStream inputStream = getContentResolver().openInputStream(childUri);
                ExifInterface exifInterface = new ExifInterface(inputStream);
                fNumber = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return fNumber;
    }

    private String getRealFilePath(Uri treeUri) {
        ContentResolver contentResolver = getContentResolver();

        String treeDocumentId = null;

        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));

        Cursor docCursor = contentResolver.query(docUri, new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);

        try {
            while (docCursor.moveToNext()) {
                treeDocumentId = docCursor.getString(2);
            }
        } finally {
            closeQuietly(docCursor);
        }


        //Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri mediaUri = MediaStore.Files.getContentUri("external");
        //Uri mediaUri = docUri;
        String[] mediaProjection = {MediaStore.Images.Media.DATA};
        String mediaSelection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
        //String[] mediaSelectionArgs = {treeDocumentId.split(":")[1]};
        String[] mediaSelectionArgs = null;
        String mediaSortOrder = null;

        Cursor mediaCursor = contentResolver.query(
                mediaUri,
                mediaProjection,
                mediaSelection,
                mediaSelectionArgs,
                mediaSortOrder);


        if (mediaCursor.moveToFirst()) {
            while (mediaCursor.moveToNext()) {

                String data = mediaCursor.getString(mediaCursor.getColumnIndex("_data"));
                if (!data.contains(getInternalStoragePath())) {
                    String[] splitDataArray = data.split("/");
                    String splitData = splitDataArray[splitDataArray.length - 1];
                    String result = data.replace(splitData, ""); //不要な部分を切り捨てる
                    return result;
                }
            }
        }

        mediaCursor.close();

        // ここにきてはいけない
        return null;

    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 内部ストレージのパス
     * @return
     */
    private String getInternalStoragePath() {
        File internalStorage = Environment.getExternalStorageDirectory();
        return internalStorage.getAbsolutePath();
    }

}
