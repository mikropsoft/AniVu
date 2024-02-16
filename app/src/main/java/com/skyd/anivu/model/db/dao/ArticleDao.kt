package com.skyd.anivu.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.ArticleBean
import com.skyd.anivu.model.bean.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.FEED_TABLE_NAME
import com.skyd.anivu.model.bean.FeedBean
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArticleDaoEntryPoint {
        val enclosureDao: EnclosureDao
    }

    @Query(
        """
        SELECT * from $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.LINK_COLUMN} = :link
        AND ${ArticleBean.FEED_URL_COLUMN} = :feedUrl
        """
    )
    suspend fun queryArticleByLink(
        link: String?,
        feedUrl: String,
    ): ArticleBean?

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun innerUpdateArticle(articleBean: ArticleBean)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun innerUpdateArticle(articleBeanList: List<ArticleBean>)

    @Transaction
    suspend fun insertListIfNotExist(articleWithEnclosureList: List<ArticleWithEnclosureBean>) {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(appContext, ArticleDaoEntryPoint::class.java)
        articleWithEnclosureList.forEach {
            if (queryArticleByLink(
                    link = it.article.link,
                    feedUrl = it.article.feedUrl,
                ) == null
            ) {
                innerUpdateArticle(it.article)
            }
            hiltEntryPoint.enclosureDao.insertListIfNotExist(it.enclosures)
        }
    }

    @Transaction
    @Delete
    suspend fun deleteArticle(articleBean: ArticleBean): Int

    @Transaction
    @Query("DELETE FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} LIKE :feedUrl")
    suspend fun deleteArticle(feedUrl: String): Int

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.FEED_URL_COLUMN} = :feedUrl
        ORDER BY ${ArticleBean.DATE_COLUMN} DESC
        """
    )
    fun getArticleList(feedUrl: String): Flow<List<ArticleBean>>

    @Transaction
    @RawQuery(observedEntities = [ArticleBean::class])
    fun getArticleList(sql: SupportSQLiteQuery): Flow<List<ArticleBean>>

    @Transaction
    @Query(
        """
        SELECT * FROM $ARTICLE_TABLE_NAME 
        WHERE ${ArticleBean.ARTICLE_ID_COLUMN} LIKE :articleId
        """
    )
    fun getArticleWithEnclosures(articleId: String): Flow<ArticleWithEnclosureBean>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT a.*
        FROM $ARTICLE_TABLE_NAME AS a LEFT JOIN $FEED_TABLE_NAME AS f 
        ON a.${ArticleBean.FEED_URL_COLUMN} = f.${FeedBean.URL_COLUMN}
        WHERE a.${ArticleBean.FEED_URL_COLUMN} = :feedUrl 
        ORDER BY date DESC LIMIT 1
        """
    )
    suspend fun queryLatestByFeedUrl(feedUrl: String): ArticleBean?
}