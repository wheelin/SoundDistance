package utilities;

import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

/**
 * <p>text-to-speech (TTS). Please note the following steps:</p>
 *
 * <ol>
 * <li>Construct the TextToSpeech object.</li>
 * <li>Handle initialization callback in the onInit method.
 * The activity implements TextToSpeech.OnInitListener for this purpose.</li>
 * <li>Call TextToSpeech.speak to synthesize speech.</li>
 * <li>Shutdown TextToSpeech in onDestroy.</li>
 * </ol>
 *
 * <p>Documentation:
 * http://developer.android.com/reference/android/speech/tts/package-summary.html
 * </p>
 * <ul>
 */

public class TextToSpeechController implements TextToSpeech.OnInitListener
{

    private static final String TAG = "TextToSpeechController";

    private TextToSpeech mTts;

    private Locale speechLocale;
    private String textToSpeak;
    private boolean queue;


    private Context context;

    private boolean isReady = false;

    /*public static TextToSpeechController getInstance(Context ctx)
    {
        if (singleton == null)
            singleton = new TextToSpeechController(ctx);

        return singleton;
    }*/

    public TextToSpeechController(Context ctx)
    {
        context = ctx;

        init();
    }

    private void init()
    {
        try
        {
            if (mTts == null)
            {
                // currently can't change Locale until speech ends
                this.speechLocale = Locale.ENGLISH;

                setReady(false);

                // Initialize text-to-speech. This is an asynchronous operation.
                // The OnInitListener (second argument) is called after initialization completes.
                try
                {
                    mTts = new TextToSpeech(context, this); // TextToSpeech.OnInitListener
                }
                catch(Exception ex)
                {
                    Log.e("TextToSpeechController", ex.getMessage());

                }
            }
        } catch (Exception e)
        {
            Log.e("TextToSpeechController", e.getMessage());
        }
    }

    public void speak(Locale speechLocale, String textToSpeak, boolean queue)
    {
        this.textToSpeak = textToSpeak;
        this.queue = queue;

        if(!isReady) {
            init();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mTts != null) {
            // currently can't change Locale until speech ends
            this.speechLocale = speechLocale;
            sayText();
        }
    }

    // Implements TextToSpeech.OnInitListener.
    @Override
    public void onInit(int status)
    {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS)
        {
            Log.d(TAG, "Speech locale:" + speechLocale);
            int result = mTts.setLanguage(speechLocale);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e(TAG, "TTS missing or not supported ("+result+")");


                // Language data is missing or the language is not supported.
                //showError(R.string.tts_lang_not_available);
            } else
            {
                // The TTS engine has been successfully initialized.                
                setReady(true);
            }
        }
    }

    private void sayText() {
        // Always set the UtteranceId (or else OnUtteranceCompleted will not be called)
        /*HashMap dummyTTSParams = new HashMap();
        dummyTTSParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AND-BIBLE"+System.currentTimeMillis());*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts.speak(this.textToSpeak,
                    queue ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH,
                    null, null);
        } else {
            mTts.speak(this.textToSpeak,
                    queue ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH,
                    null);
        }
    }

    public void stopTTSController()
    {
        if (mTts != null)
        {
            mTts.stop();
            mTts.shutdown();
            //mTts = null;
        }
    }

    public void setReady(boolean isReady)
    {
        this.isReady = isReady;
    }
 
    /*
    @Override
    public void applicationNowInBackground(AppToBackgroundEvent e) {
        stop();    
    }
    */
}