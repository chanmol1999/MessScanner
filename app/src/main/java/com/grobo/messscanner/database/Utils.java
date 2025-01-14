package com.grobo.messscanner.database;

import android.os.AsyncTask;

public class Utils {

    public static class UserCountTask extends AsyncTask<String, Void, Integer> {
        private UserDao mAsyncTaskDao;
        public UserCountTask(UserDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Integer doInBackground(String... params) {
            return mAsyncTaskDao.getUserCount(params[0]);
        }
    }

    public static class InsertUser extends AsyncTask<UserModel, Void, Void> {
        private UserDao mAsyncTaskDao;
        public InsertUser(UserDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(UserModel... params) {
            mAsyncTaskDao.insertUser(params[0]);
            return null;
        }
    }

    public static class LoadUserByMongoId extends AsyncTask<String, Void, UserModel> {
        private UserDao mAsyncTaskDao;
        public LoadUserByMongoId(UserDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected UserModel doInBackground(String... params) {
            return mAsyncTaskDao.loadUserByMongoId(params[0]);
        }
    }
}
