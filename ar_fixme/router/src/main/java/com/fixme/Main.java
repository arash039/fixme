package com.fixme;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        final Router router = new Router();
        Executor pool = Executors.newFixedThreadPool(2);
        pool.execute(new Runnable() {
            @Override
            public void run() {
                router.acceptBroker();
            }
        });
        pool.execute(new Runnable() {
            @Override
            public void run() {
                router.acceptMarket();
            }
        });
    }
}