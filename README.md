#jenkins master节点部署使用

### 安装jdk

```shell
sudo wget -O /etc/yum.repos.d/jenkins.repo \
    https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
sudo yum install java-11-openjdk
```

### jenkins启停

```shell
[root@master opt]# tree -L 2 /opt/jenkins/
/opt/jenkins/
├── jenkins.war
├── nohup.out
└── start.sh

0 directories, 3 files

#启用
[root@master opt]# cat /opt/jenkins/start.sh 
nohup /usr/lib/jvm/java-11-openjdk-11.0.18.0.10-1.el7_9.x86_64/bin/java -jar /opt/jenkins/jenkins.war --httpPort=30020 --argumentsRealm.passwd.$USER=Zetyun@1 &

#停止
[root@master opt]# cat /opt/jenkins/stop.sh 
#!/bin/bash 

ps aux |grep jenkins|awk '{print $2}'|xargs kill
```

### 插件安装
```txt
kubernetes-cli
```
### 系统配置
```txt
添加凭据
    gitlab、harbor、k8s-config
允许非注册用户访问流水线执行结果
    系统管理->全局安全设置->勾选匿名用户具有可读权限
```

### 提取jar/disz到行内部署
```txt
jar
    cd /mjmp/nfs-provisioner/cicd-pvc-maven-pvc-1b2bdb19-c630-495a-a65a-32951c5c313e
dist
    cd /mjmp/nfs-provisioner/cicd-pvc-yarn-pvc-794a04eb-7c84-44c4-81f9-ed65d1a35dcc/bsd-web
```
