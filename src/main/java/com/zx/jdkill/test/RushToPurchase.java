package com.zx.jdkill.test;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author: zhaoxu
 * @date: 2021/1/8 20:51
 */
public class RushToPurchase implements Runnable {
    //请求头
    static Map<String, List<String>> stringListMap = new HashMap<String, List<String>>();
    volatile static Integer times = 0;
    volatile static Boolean purchase = true;
    static Logger log = LoggerFactory.getLogger(RushToPurchase.class);

    public void run() {
        JSONObject headers = new JSONObject();
        headers.put(Start.headerAgent, Start.headerAgentArg);
        headers.put(Start.Referer, Start.RefererArg);
        while (purchase) {
            //获取ip，使用的是免费的 携趣代理 ，不需要或者不会用可以注释掉
            if (!"".equals(Start.getIpUrl)) {
                setIpProxy();
            }
            //抢购
            String gate = null;
            List<String> cookie = new ArrayList<>();
            try {
                synchronized (times) {
                    if (times < Start.ok) {
                        gate = HttpUrlConnectionUtil.get(headers, "https://cart.jd.com/gate.action?pcount=1&ptype=1&pid=" + Start.pid);
                        if (!gate.contains("商品已成功加入")) {
                            log.info("获取商品信息失败");
                            continue;
                        } else {
                            times++;
                            continue;
                        }
                    }
                }
            } catch (IOException e) {
            }
//            //订单信息
//            stringListMap.clear();
//            try {
//                stringListMap = Start.manager.get(new URI("https://trade.jd.com/shopping/order/getOrderInfo.action"), stringListMap);
//            } catch (URISyntaxException e) {
//                e.printStackTrace();
//            }
//            cookie = stringListMap.get("Cookie");
//            headers.put("Cookie", cookie.get(0).toString());
            try {
                String orderInfo = HttpUrlConnectionUtil.get(headers, "https://trade.jd.com/shopping/order/getOrderInfo.action");
            } catch (IOException e) {
                e.printStackTrace();
            }

            //提交订单
            JSONObject subData = new JSONObject();
            headers = new JSONObject();
            subData.put("overseaPurchaseCookies", "");
            subData.put("vendorRemarks", "[]");
            subData.put("submitOrderParam.sopNotPutInvoice", "false");
            subData.put("submitOrderParam.ignorePriceChange", "1");
            subData.put("submitOrderParam.btSupport", "0");
            subData.put("submitOrderParam.isBestCoupon", "1");
            subData.put("submitOrderParam.jxj", "1");
            subData.put("submitOrderParam.trackID", Login.ticket);
            subData.put("submitOrderParam.eid", Start.eid);
            subData.put("submitOrderParam.fp", Start.fp);
            subData.put("submitOrderParam.needCheck", "1");

            headers.put("Referer", "http://trade.jd.com/shopping/order/getOrderInfo.action");
            headers.put("origin", "https://trade.jd.com");
            headers.put("Content-Type", "application/json");
            headers.put("x-requested-with", "XMLHttpRequest");
            headers.put("upgrade-insecure-requests", "1");
            headers.put("sec-fetch-user", "?1");
            stringListMap.clear();

            try {
                stringListMap = Start.manager.get(new URI("https://trade.jd.com/shopping/order/getOrderInfo.action"), stringListMap);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            cookie = stringListMap.get("Cookie");
            headers.put("Cookie", cookie.get(0).toString());
            String submitOrder = null;
            try {
                if (times >= Start.ok) {
                    submitOrder = HttpUrlConnectionUtil.post(headers, "https://trade.jd.com/shopping/order/submitOrder.action", null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (submitOrder.contains("刷新太频繁了") || submitOrder.contains("抱歉，您访问的内容不存在")) {
                log.info("刷新太频繁了,您访问的内容不存在");
                continue;
            }
            JSONObject jsonObject = JSONObject.parseObject(submitOrder);
            String success = null;
            String message = null;
            if (jsonObject != null && jsonObject.get("success") != null) {
                success = jsonObject.get("success").toString();
            }
            if (jsonObject != null && jsonObject.get("message") != null) {
                message = jsonObject.get("message").toString();
            }
            if (times >= Start.ok) {
                if ("true".equals(success)) {
                    log.info("已成功抢购" + times + "件，请尽快完成付款");
                    purchase = false;
                } else {
                    if (message != null) {
                        log.info(message);
                    } else if (submitOrder.contains("很遗憾没有抢到")) {
                        log.info("很遗憾没有抢到，再接再厉哦");
                    } else if (submitOrder.contains("抱歉，您提交过快，请稍后再提交订单！")) {
                        log.info("抱歉，您提交过快，请稍后再提交订单！");
                    } else if (submitOrder.contains("系统正在开小差，请重试~~")) {
                        log.info("系统正在开小差，请重试~~");
                    } else if (submitOrder.contains("您多次提交过快")) {
                        log.info("您多次提交过快，请稍后再试");
                    } else {
                        log.info("获取用户订单信息失败");
                    }
                }
            }
        }
    }

    public static void setIpProxy() {
        JSONObject headers = new JSONObject();
        headers.put(Start.headerAgent, Start.headerAgentArg);
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            System.getProperties().setProperty("http.proxyHost", hostAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int rand = (int) ((Math.random() * (100 - 0 + 1)) + 0);
        String[] r1 = HttpUrlConnectionUtil.ips.get(rand).split(":");
        if (HttpUrlConnectionUtil.ips.size() == 0 && r1[1].length() > 5) {
            return;
        } else {
            System.getProperties().setProperty("http.proxyHost", r1[0]);
            System.getProperties().setProperty("http.proxyPort", r1[1]);
        }
//        System.err.println(r1[0] + ":" + r1[1]);
    }
}
