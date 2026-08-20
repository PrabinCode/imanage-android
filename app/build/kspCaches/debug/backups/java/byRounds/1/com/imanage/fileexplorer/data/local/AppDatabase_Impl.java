package com.imanage.fileexplorer.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.imanage.fileexplorer.data.local.dao.BookmarkDao;
import com.imanage.fileexplorer.data.local.dao.BookmarkDao_Impl;
import com.imanage.fileexplorer.data.local.dao.SearchHistoryDao;
import com.imanage.fileexplorer.data.local.dao.SearchHistoryDao_Impl;
import com.imanage.fileexplorer.data.local.dao.TrashDao;
import com.imanage.fileexplorer.data.local.dao.TrashDao_Impl;
import com.imanage.fileexplorer.data.local.dao.VaultDao;
import com.imanage.fileexplorer.data.local.dao.VaultDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile BookmarkDao _bookmarkDao;

  private volatile TrashDao _trashDao;

  private volatile VaultDao _vaultDao;

  private volatile SearchHistoryDao _searchHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`path` TEXT NOT NULL, `title` TEXT NOT NULL, `isDirectory` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`path`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `trash_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `originalPath` TEXT NOT NULL, `originalName` TEXT NOT NULL, `trashedPath` TEXT NOT NULL, `trashedTime` INTEGER NOT NULL, `size` INTEGER NOT NULL, `isDirectory` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `vault_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `encryptedFileName` TEXT NOT NULL, `originalFileName` TEXT NOT NULL, `originalPath` TEXT NOT NULL, `fileSize` INTEGER NOT NULL, `encryptedAt` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `isDirectory` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`query`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '712bec68c8d4c108773dab697f31e10d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `bookmarks`");
        db.execSQL("DROP TABLE IF EXISTS `trash_items`");
        db.execSQL("DROP TABLE IF EXISTS `vault_items`");
        db.execSQL("DROP TABLE IF EXISTS `search_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBookmarks = new HashMap<String, TableInfo.Column>(4);
        _columnsBookmarks.put("path", new TableInfo.Column("path", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("isDirectory", new TableInfo.Column("isDirectory", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBookmarks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBookmarks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBookmarks = new TableInfo("bookmarks", _columnsBookmarks, _foreignKeysBookmarks, _indicesBookmarks);
        final TableInfo _existingBookmarks = TableInfo.read(db, "bookmarks");
        if (!_infoBookmarks.equals(_existingBookmarks)) {
          return new RoomOpenHelper.ValidationResult(false, "bookmarks(com.imanage.fileexplorer.data.local.entity.BookmarkEntity).\n"
                  + " Expected:\n" + _infoBookmarks + "\n"
                  + " Found:\n" + _existingBookmarks);
        }
        final HashMap<String, TableInfo.Column> _columnsTrashItems = new HashMap<String, TableInfo.Column>(7);
        _columnsTrashItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("originalPath", new TableInfo.Column("originalPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("originalName", new TableInfo.Column("originalName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("trashedPath", new TableInfo.Column("trashedPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("trashedTime", new TableInfo.Column("trashedTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("size", new TableInfo.Column("size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrashItems.put("isDirectory", new TableInfo.Column("isDirectory", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrashItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrashItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrashItems = new TableInfo("trash_items", _columnsTrashItems, _foreignKeysTrashItems, _indicesTrashItems);
        final TableInfo _existingTrashItems = TableInfo.read(db, "trash_items");
        if (!_infoTrashItems.equals(_existingTrashItems)) {
          return new RoomOpenHelper.ValidationResult(false, "trash_items(com.imanage.fileexplorer.data.local.entity.TrashEntity).\n"
                  + " Expected:\n" + _infoTrashItems + "\n"
                  + " Found:\n" + _existingTrashItems);
        }
        final HashMap<String, TableInfo.Column> _columnsVaultItems = new HashMap<String, TableInfo.Column>(8);
        _columnsVaultItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("encryptedFileName", new TableInfo.Column("encryptedFileName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("originalFileName", new TableInfo.Column("originalFileName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("originalPath", new TableInfo.Column("originalPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("fileSize", new TableInfo.Column("fileSize", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("encryptedAt", new TableInfo.Column("encryptedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("mimeType", new TableInfo.Column("mimeType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVaultItems.put("isDirectory", new TableInfo.Column("isDirectory", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVaultItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVaultItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVaultItems = new TableInfo("vault_items", _columnsVaultItems, _foreignKeysVaultItems, _indicesVaultItems);
        final TableInfo _existingVaultItems = TableInfo.read(db, "vault_items");
        if (!_infoVaultItems.equals(_existingVaultItems)) {
          return new RoomOpenHelper.ValidationResult(false, "vault_items(com.imanage.fileexplorer.data.local.entity.VaultEntity).\n"
                  + " Expected:\n" + _infoVaultItems + "\n"
                  + " Found:\n" + _existingVaultItems);
        }
        final HashMap<String, TableInfo.Column> _columnsSearchHistory = new HashMap<String, TableInfo.Column>(2);
        _columnsSearchHistory.put("query", new TableInfo.Column("query", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSearchHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSearchHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSearchHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSearchHistory = new TableInfo("search_history", _columnsSearchHistory, _foreignKeysSearchHistory, _indicesSearchHistory);
        final TableInfo _existingSearchHistory = TableInfo.read(db, "search_history");
        if (!_infoSearchHistory.equals(_existingSearchHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "search_history(com.imanage.fileexplorer.data.local.entity.SearchHistoryEntity).\n"
                  + " Expected:\n" + _infoSearchHistory + "\n"
                  + " Found:\n" + _existingSearchHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "712bec68c8d4c108773dab697f31e10d", "7a27f5a9bed8e0e54d3c61054ad5ff8f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "bookmarks","trash_items","vault_items","search_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `bookmarks`");
      _db.execSQL("DELETE FROM `trash_items`");
      _db.execSQL("DELETE FROM `vault_items`");
      _db.execSQL("DELETE FROM `search_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BookmarkDao.class, BookmarkDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrashDao.class, TrashDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(VaultDao.class, VaultDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SearchHistoryDao.class, SearchHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BookmarkDao bookmarkDao() {
    if (_bookmarkDao != null) {
      return _bookmarkDao;
    } else {
      synchronized(this) {
        if(_bookmarkDao == null) {
          _bookmarkDao = new BookmarkDao_Impl(this);
        }
        return _bookmarkDao;
      }
    }
  }

  @Override
  public TrashDao trashDao() {
    if (_trashDao != null) {
      return _trashDao;
    } else {
      synchronized(this) {
        if(_trashDao == null) {
          _trashDao = new TrashDao_Impl(this);
        }
        return _trashDao;
      }
    }
  }

  @Override
  public VaultDao vaultDao() {
    if (_vaultDao != null) {
      return _vaultDao;
    } else {
      synchronized(this) {
        if(_vaultDao == null) {
          _vaultDao = new VaultDao_Impl(this);
        }
        return _vaultDao;
      }
    }
  }

  @Override
  public SearchHistoryDao searchHistoryDao() {
    if (_searchHistoryDao != null) {
      return _searchHistoryDao;
    } else {
      synchronized(this) {
        if(_searchHistoryDao == null) {
          _searchHistoryDao = new SearchHistoryDao_Impl(this);
        }
        return _searchHistoryDao;
      }
    }
  }
}
