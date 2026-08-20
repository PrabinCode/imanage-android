package com.imanage.fileexplorer.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.imanage.fileexplorer.data.local.entity.TrashEntity;
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
public final class TrashDao_Impl implements TrashDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrashEntity> __insertionAdapterOfTrashEntity;

  private final EntityDeletionOrUpdateAdapter<TrashEntity> __deletionAdapterOfTrashEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearTrash;

  public TrashDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrashEntity = new EntityInsertionAdapter<TrashEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `trash_items` (`id`,`originalPath`,`originalName`,`trashedPath`,`trashedTime`,`size`,`isDirectory`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrashEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getOriginalPath());
        statement.bindString(3, entity.getOriginalName());
        statement.bindString(4, entity.getTrashedPath());
        statement.bindLong(5, entity.getTrashedTime());
        statement.bindLong(6, entity.getSize());
        final int _tmp = entity.isDirectory() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__deletionAdapterOfTrashEntity = new EntityDeletionOrUpdateAdapter<TrashEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `trash_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrashEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM trash_items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM trash_items";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrashItem(final TrashEntity item,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrashEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTrashItem(final TrashEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTrashEntity.handle(item);
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
  public Object clearTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearTrash.acquire();
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
          __preparedStmtOfClearTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TrashEntity>> getAllTrashItems() {
    final String _sql = "SELECT * FROM trash_items ORDER BY trashedTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"trash_items"}, new Callable<List<TrashEntity>>() {
      @Override
      @NonNull
      public List<TrashEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOriginalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPath");
          final int _cursorIndexOfOriginalName = CursorUtil.getColumnIndexOrThrow(_cursor, "originalName");
          final int _cursorIndexOfTrashedPath = CursorUtil.getColumnIndexOrThrow(_cursor, "trashedPath");
          final int _cursorIndexOfTrashedTime = CursorUtil.getColumnIndexOrThrow(_cursor, "trashedTime");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfIsDirectory = CursorUtil.getColumnIndexOrThrow(_cursor, "isDirectory");
          final List<TrashEntity> _result = new ArrayList<TrashEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrashEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpOriginalPath;
            _tmpOriginalPath = _cursor.getString(_cursorIndexOfOriginalPath);
            final String _tmpOriginalName;
            _tmpOriginalName = _cursor.getString(_cursorIndexOfOriginalName);
            final String _tmpTrashedPath;
            _tmpTrashedPath = _cursor.getString(_cursorIndexOfTrashedPath);
            final long _tmpTrashedTime;
            _tmpTrashedTime = _cursor.getLong(_cursorIndexOfTrashedTime);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final boolean _tmpIsDirectory;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDirectory);
            _tmpIsDirectory = _tmp != 0;
            _item = new TrashEntity(_tmpId,_tmpOriginalPath,_tmpOriginalName,_tmpTrashedPath,_tmpTrashedTime,_tmpSize,_tmpIsDirectory);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
