/**
 * 对 stomp 客户端进行封装
 */

var client;
var subscribes = [];
var errorTimes = 0;

var endpoint = "/ws";

/**
 * 建立websocket连接
 * @param {Function} onConnecting 开始连接时的回调
 * @param {Function} onConnected 连接成功回调
 * @param {Function} onError 连接异常或断开回调
 */
function connect(onConnecting, onConnected, onError) {
    onConnecting instanceof Function && onConnecting();
    var sock = new SockJS(endpoint);
    client = Stomp.over(sock);
    console.log("ws: start connect to " + endpoint);
    client.connect({}, function (frame) {
        errorTimes = 0;
        console.log('connected: ' + frame);
        // 连接成功后重新订阅
        subscribes.forEach(function (item) {
            client.subscribe(item.destination, function (resp) {
                console.debug("ws收到消息: ", resp);
                item.cb(JSON.parse(resp.body));
            });
        });
        onConnected instanceof Function && onConnected();
    }, function (err) {
        errorTimes = errorTimes > 8 ? 0 : errorTimes;
        var nextTime = ++errorTimes * 3000;
        console.warn("与服务器断开连接，" + nextTime + " 秒后重新连接", err);
        setTimeout(function () {
            console.log("尝试重连……");
            connect(onConnecting, onConnected, onError);
        }, nextTime);
        onError instanceof Function && onError();
    });
}

/**
 * 订阅消息，若当前未连接，则会在连接成功后自动订阅
 *
 * 注意，为防止重连导致重复订阅，请勿使用匿名函数做回调
 *
 * @param {String} destination 目标
 * @param {Function} cb 回调
 */
function subscribe(destination, cb) {
    var exist = subscribes.filter(function (sub) {
        return sub.destination === destination && sub.cb === cb
    });
    // 防止重复订阅
    if (exist && exist.length) {
        return;
    }
    // 记录所有订阅，在连接成功时统一处理
    subscribes.push({
        destination: destination,
        cb: cb
    });
    if (client && client.connected) {
        client.subscribe(destination, function (resp) {
            console.debug("ws收到消息: ", resp);
            cb instanceof Function && cb(JSON.parse(resp.body));
        });
    } else {
        console.warn("ws未连接，暂时无法订阅：" + destination)
    }
}

/**
 * 发送消息
 * @param {String} destination 目标
 * @param {Object} msg 消息体对象
 */
function send(destination, msg) {
    if (!client) {
        console.error("客户端未连接，无法发送消息！")
    }
    client.send(destination, {}, JSON.stringify(msg));
}

window.onbeforeunload = function () {
    // 当窗口关闭时断开连接
    if (client && client.connected) {
        client.disconnect(function () {
            console.log("websocket disconnected ");
        });
    }
};