package pro.jsan.hermes.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import pro.jsan.hermes.data.SettingsRepository
import pro.jsan.hermes.data.db.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "hermes.db").build()

    @Provides
    fun provideSyncRuleDao(db: AppDatabase) = db.syncRuleDao()

    @Provides
    fun provideSyncedFileDao(db: AppDatabase) = db.syncedFileDao()

    @Provides @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context) = SettingsRepository(ctx)

    @Provides @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }
}
