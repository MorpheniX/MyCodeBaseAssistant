package ee.carlrobert.codegpt.completions;

import static ee.carlrobert.codegpt.credentials.CredentialsStore.getCredential;

import com.intellij.openapi.application.ApplicationManager;
import ee.carlrobert.codegpt.credentials.CredentialsStore.CredentialKey;
import ee.carlrobert.codegpt.settings.advanced.AdvancedSettings;
import ee.carlrobert.codegpt.settings.service.openai.OpenAISettings;
import ee.carlrobert.llm.client.codegpt.CodeGPTClient;
import ee.carlrobert.llm.client.openai.OpenAIClient;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

public class CompletionClientProvider {

  public static CodeGPTClient getCodeGPTClient() {
    return new CodeGPTClient(
        getCredential(CredentialKey.CodeGptApiKey.INSTANCE),
        getDefaultClientBuilder());
  }

  public static OpenAIClient getOpenAIClient() {
    return new OpenAIClient.Builder(getCredential(CredentialKey.OpenaiApiKey.INSTANCE))
        .setOrganization(OpenAISettings.getCurrentState().getOrganization())
        .build(getDefaultClientBuilder());
  }

  public static OkHttpClient.Builder getDefaultClientBuilder() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    var advancedSettings = AdvancedSettings.getCurrentState();
    var proxyHost = advancedSettings.getProxyHost();
    var proxyPort = advancedSettings.getProxyPort();
    if (!proxyHost.isEmpty() && proxyPort != 0) {
      builder.proxy(
          new Proxy(advancedSettings.getProxyType(), new InetSocketAddress(proxyHost, proxyPort)));
      if (advancedSettings.isProxyAuthSelected()) {
        builder.proxyAuthenticator((route, response) ->
            response.request()
                .newBuilder()
                .header("Proxy-Authorization", Credentials.basic(
                    advancedSettings.getProxyUsername(),
                    advancedSettings.getProxyPassword()))
                .build());
      }
    }

    // SSL certificate verification disabling - only if enabled in settings
    if (advancedSettings.isDisableSslVerification()) {
      try {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
              }

              @Override
              public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
              }

              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
              }
            }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        // Create an ssl socket factory with our all-trusting manager
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return builder
        .connectTimeout(advancedSettings.getConnectTimeout(), TimeUnit.SECONDS)
        .readTimeout(advancedSettings.getReadTimeout(), TimeUnit.SECONDS);
  }
}
