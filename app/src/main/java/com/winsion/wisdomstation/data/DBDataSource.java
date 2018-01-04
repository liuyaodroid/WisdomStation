package com.winsion.wisdomstation.data;

import android.text.TextUtils;

import com.winsion.wisdomstation.application.AppApplication;
import com.winsion.wisdomstation.login.entity.UserEntity;
import com.winsion.wisdomstation.login.entity.UserEntity_;
import com.winsion.wisdomstation.modules.reminder.entity.TodoEntity;
import com.winsion.wisdomstation.modules.reminder.entity.TodoEntity_;

import java.util.List;

import io.objectbox.Box;

/**
 * Created by wyl on 2017/3/21.
 * 本地数据库数据
 */

public class DBDataSource {
    private static volatile DBDataSource mInstance;
    private final Box<UserEntity> mUserEntityBox;
    private final Box<TodoEntity> mTodoEntityBox;

    private DBDataSource() {
        mUserEntityBox = AppApplication.getBoxStore().boxFor(UserEntity.class);
        mTodoEntityBox = AppApplication.getBoxStore().boxFor(TodoEntity.class);
    }

    public static DBDataSource getInstance() {
        if (mInstance == null) {
            synchronized (DBDataSource.class) {
                if (mInstance == null) {
                    mInstance = new DBDataSource();
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取所有登录过的用户，按照登录时间降序排序
     *
     * @return 用户集合
     */
    public List<UserEntity> getAllSavedUser() {
        return mUserEntityBox.query()
                .orderDesc(UserEntity_.lastLoginTime)
                .build()
                .find();
    }

    public UserEntity getUserByUsername(String username) {
        return mUserEntityBox.query()
                .equal(UserEntity_.username, username)
                .build()
                .findUnique();
    }

    public UserEntity getLastLoginUser() {
        return mUserEntityBox.query()
                .orderDesc(UserEntity_.lastLoginTime)
                .build()
                .findFirst();
    }

    public void deleteUser(UserEntity userEntity) {
        mUserEntityBox.remove(userEntity);
    }

    /**
     * 存储用户信息到数据库
     */
    public void saveUserInfo(UserEntity userEntity) {
        String username = userEntity.getUsername();
        UserEntity user = getUserByUsername(username);
        if (user != null) {
            userEntity.setId(user.getId());
        }
        mUserEntityBox.put(userEntity);
        // 把其他用户的自动登录置为false
        List<UserEntity> allSavedUser = getAllSavedUser();
        for (UserEntity entity : allSavedUser) {
            if (!TextUtils.equals(entity.getUsername(), username)) {
                entity.setIsAutoLogin(false);
                mUserEntityBox.put(entity);
            }
        }
    }

    /**
     * 取消当前用户的自动登录
     */
    public void cancelAutoLogin() {
        UserEntity lastLoginUser = getLastLoginUser();
        if (lastLoginUser != null) {
            lastLoginUser.setIsAutoLogin(false);
            mUserEntityBox.put(lastLoginUser);
        }
    }

    /**
     * 删除一条代办事项
     */
    public void deleteOneTodo(TodoEntity todoEntity) {
        mTodoEntityBox.remove(todoEntity.getId());
    }

    /**
     * 根据完成状态查询代办事项
     *
     * @param finishStatus 完成状态
     * @param userId       当前用户ID
     * @return 符合条件的待办事项集合
     */
    public List<TodoEntity> queryTodoByStatus(boolean finishStatus, String userId) {
        return mTodoEntityBox.query()
                .equal(TodoEntity_.finished, finishStatus)
                .equal(TodoEntity_.belongUserId, userId)
                .order(TodoEntity_.planDate)
                .build()
                .find();
    }

    public TodoEntity getTodoById(long todoId) {
        return mTodoEntityBox.query()
                .equal(TodoEntity_.id, todoId)
                .build()
                .findUnique();
    }

    public void updateOrAddTodo(TodoEntity todoEntity) {
        mTodoEntityBox.put(todoEntity);
    }
}