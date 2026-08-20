package com.imanage.fileexplorer.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.imanage.fileexplorer.data.local.entity.VaultEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VaultDao_Impl implements VaultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VaultEntity> __insertionAdapterOfVaultEntity;

  private final EntityDeletionOrUpdateAdapter<VaultEntity> __deletionAdapterOfVaultEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public VaultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVaultEntity = new EntityInsertionAdapter<VaultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `vault_items` (`id`,`encryptedFileName`,`originalFileName`,`originalPath`,`fileSize`,`encryptedAt`,`mimeType`,`isDirectory`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VaultEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getEncryptedFileName());
        statement.bindString(3, entity.getOriginalFileName());
        statement.bindString(4, entity.getOriginalPath());
        statement.bindLong(5, entity.getFileSize());
        statement.bindLong(6, entity.getEncryptedAt());
        statement.bindString(7, entity.getMimeType());
        final int _tmp = entity.isDirectory() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__deletionAdapterOfVaultEntity = new EntityDeletionOrUpdateAdapter<VaultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `vault_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VaultEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM vault_items WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertVaultItem(final VaultEntity item,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfVaultEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteVaultItem(final VaultEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfVaultEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VaultEntity>> getAllVaultItems() {
    final String _sql = "SELECT * FROM vault_items ORDER BY encryptedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vault_items"}, new Callable<List<VaultEntity>>() {
      @Override
      @NonNull
      public List<VaultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEncryptedFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedFileName");
          final int _cursorIndexOfOriginalFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "originalFileName");
          final int _cursorIndexOfOriginalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPath");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfEncryptedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedAt");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfIsDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectory");
          final List<VaultEntity> _result = new ArrayList<VaultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VaultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEncryptedFileName;
            _tmpEncryptedFileName = _cursor.getString(_cursorIndexOfEncryptedFileName);
            final String _tmpOriginalFileName;
            _tmpOriginalFileName = _cursor.getString(_cursorIndexOfOriginalFileName);
            final String _tmpOriginalPath;
            _tmpOriginalPath = _cursor.getString(_cursorIndexOfOriginalPath);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpEncryptedAt;
            _tmpEncryptedAt = _cursor.getLong(_cursorIndexOfEncryptedAt);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final boolean _tmpIsDirectory;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectory);
            _tmpIsDirectory = _tmp != 0;
            _item = new VaultEntity(_tmpId,_tmpEncryptedFileName,_tmpOriginalFileName,_tmpOriginalPath,_tmpFileSize,_tmpEncryptedAt,_tmpMimeType,_tmpIsDirectory);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super VaultEntity> $completion) {
    final String _sql = "SELECT * FROM vault_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VaultEntity>() {
      @Override
      @Nullable
      public VaultEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEncryptedFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedFileName");
          final int _cursorIndexOfOriginalFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "originalFileName");
          final int _cursorIndexOfOriginalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPath");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfEncryptedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "encryptedAt");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfIsDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectory");
          final VaultEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpEncryptedFileName;
            _tmpEncryptedFileName = _cursor.getString(_cursorIndexOfEncryptedFileName);
            final String _tmpOriginalFileName;
            _tmpOriginalFileName = _cursor.getString(_cursorIndexOfOriginalFileName);
            final String _tmpOriginalPath;
            _tmpOriginalPath = _cursor.getString(_cursorIndexOfOriginalPath);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final long _tmpEncryptedAt;
            _tmpEncryptedAt = _cursor.getLong(_cursorIndexOfEncryptedAt);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final boolean _tmpIsDirectory;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectory);
            _tmpIsDirectory = _tmp != 0;
            _result = new VaultEntity(_tmpId,_tmpEncryptedFileName,_tmpOriginalFileName,_tmpOriginalPath,_tmpFileSize,_tmpEncryptedAt,_tmpMimeType,_tmpIsDirectory);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
