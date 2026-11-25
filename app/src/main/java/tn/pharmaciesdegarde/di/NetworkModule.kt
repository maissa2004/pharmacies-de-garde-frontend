package tn.pharmaciesdegarde.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import tn.pharmaciesdegarde.data.local.TokenStore
import tn.pharmaciesdegarde.data.remote.ApiService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://172.16.24.102:8081/api/"
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides @Singleton
    fun provideOkHttp(tokenStore: TokenStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val auth = Interceptor { chain ->
            val token = runBlocking { tokenStore.token.first() }
            val request = if (token != null)
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            else chain.request()
            chain.proceed(request)
        }
        return OkHttpClient.Builder().addInterceptor(auth).addInterceptor(logging).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}