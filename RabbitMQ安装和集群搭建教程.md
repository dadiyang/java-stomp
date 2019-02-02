[文章链接](https://blog.csdn.net/dadiyang/article/details/85774577)
# 安装包下载

## erlang
下载安装包：

* CentOs6下载 [erlang-21.2.2-1.el6.x86_64.rpm](https://github.com/rabbitmq/erlang-rpm/releases/download/v21.2.2/erlang-21.2.2-1.el6.x86_64.rpm)
* CentOs7下载 [erlang-21.2.2-1.el7.centos.x86_64.rpm](https://github.com/rabbitmq/erlang-rpm/releases/download/v21.2.2/erlang-21.2.2-1.el7.centos.x86_64.rpm)

## 签名key文件
从 [GitHub release](https://github.com/rabbitmq/signing-keys/releases/) 页面中下载 [rabbitmq-release-signing-key.asc](https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc)

## rabbitmq rpm安装包
在 rabbitmq-server 的 [GitHub release](https://github.com/rabbitmq/rabbitmq-server/releases) 页面中选择合适的版本并选择适合的安装包，当前最新的release版本是3.7.9 (2019年1月)，则：

* CentOs6下载 [rabbitmq-server-3.7.9-1.el6.noarch.rpm](https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.7.9/rabbitmq-server-3.7.9-1.el6.noarch.rpm)
* CentOs7下载 [rabbitmq-server-3.7.9-1.el7.noarch.rpm](https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.7.9/rabbitmq-server-3.7.9-1.el7.noarch.rpm)

# 安装

安装包准备好之后，我们来安装（**yum命令需要root权限**）

```shell
# 安装 erlang
rpm -ivh erlang-21.1.4-1.el6.x86_64.rpm
# 导入签名
rpm --import rabbitmq-release-signing-key.asc
# 安装 rabbitmq
yum install -y rabbitmq-server-3.7.9-1.el6.noarch.rpm
```

如果在 install 的时候报下面的异常

* groupadd: cannot open /etc/group
* useradd: cannot open /etc/passwd
* useradd: cannot open /etc/shadow

则对应地执行这三条命令然后再 install 即可

```shell
chattr -i /etc/group
chattr -i /etc/passwd
chattr -i /etc/shadow
```

# 配置

## 管理插件

`rabbitmq-plugins enable rabbitmq_management`

开始管理插件后可以登录 http://localhost:15672 来管理rabbitmq, 默认账号密码都为guest

## STOMP 插件

`rabbitmq-plugins enable rabbitmq_stomp`

## 添加用户

格式: `rabbitmqctl add_user <user> <password>`
示例: `rabbitmqctl add_user test Passw0rd`

## 添加vhost

格式: `rabbitmqctl add_vhost <hostname>`
示例: `rabbitmqctl add_vhost /wxkf`

## 用户授权

### 角色

格式: `rabbitmqctl  set_user_tags  <user>  <role>`
示例: `rabbitmqctl  set_user_tags  test  administrator`

### vhost授权

格式: `rabbitmqctl set_permissions [-p <vhostpath>] <user> <conf> <write> <read>`
示例: `rabbitmqctl set_permissions -p /wxkf test ".*" ".*" ".*"`

# 非 root 权限启动 rabbitmq

安装完成之后 rabbitmq 会在 /usr/sbin/ 目录下加入所需要的命令，如 rabbitmq-server，默认只以rabbitmq用户启动，而使用该用户需要root权限。但是我们在生产机器上不可能一直使用 root，因此使用普通用户启动 rabbitmq 非常有必要。

**只需要两步就可以实现**

1. 删除 /usr/sbin/rabbitmq*
```shell
rm -f /usr/sbin/rabbitmq*
```
2. 添加软链
```shell
# 注意：rabbitmq_server-3.7.9 换成你安装的对应版本
ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmqctl /usr/sbin/rabbitmqctl
 ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmq-env /usr/sbin/rabbitmq-env
 ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmq-server /usr/sbin/rabbitmq-server
 ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmq-defaults /usr/sbin/rabbitmq-defaults
ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmq-diagnostics /usr/sbin/rabbitmq-diagnostics
ln -s /usr/lib/rabbitmq/lib/rabbitmq_server-3.7.9/sbin/rabbitmq-plugins /usr/sbin/rabbitmq-plugins
```

# 集群搭建

## .erlang.cookie 文件

将第一台RabbitMQ的 `~/.erlang.cookie` 文件复制替换掉其他的机器相同路径下的文件，以确保各个节点的cookie文件使用的是同一个值，节点之间通过cookie确定相互是否可通信。

**注意：如果是直接修改cookie文件的内容，需要修改权限。** .erlang.cookie 文件默认权限是 400，即只有owner才有只读权限，执行 `chmod +w .erlang.cookie` 添加写权限，改完之后执行 `chmod -w .erlang.cookie` 改回原来的权限

## 配置各节点的hosts文件

`sudo vim /etc/hosts`

```shell
# 格式：ip 节点名
xxx.xxx.xxx.xxx rmq-broker-test-1
xxx.xxx.xxx.xxx rmq-broker-test-2
xxx.xxx.xxx.xxx rmq-broker-test-3
```
注意：节点名称为机器的 hostname，可以通过执行 hostname 命令查看（如果全称包含 . 会被截断，只取 . 前面的部分，如hostname 为 bjdhj-185-73.58os.org 则节点名称为bjdhj-185-73）
以下 node1、node2表示节点名称

# 组成集群

1. 先启动任意一个节点，以 node1 为例，在 node1 机器上执行
```shell
rabbit-server -detached
```
2. 再启动其他机器，以 node2 为例，在 node2 机器上执行
```shell
# 启动 rabbit-server
rabbit-server -detached
# 停止rabbitmq服务（这命令只是停止对外服务，进程还在）
rabbitmqctl stop_app
# 加入集群，集群格式为 rabbit@节点名
rabbitmqctl join_cluster rabbit@node1
# 开启rabbitmq服务
rabbitmqctl start_app
```
集群搭建完成之后可以通过web管理界面查看集群集群信息，地址为任意节点的ip，端口号为 15672，如 http://locahost:15672

集群部分参考：[CentOs7.3 搭建 RabbitMQ 3.6 Cluster 集群服务与使用](https://segmentfault.com/a/1190000010702020)
