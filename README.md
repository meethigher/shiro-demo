---
title: Shiro
comments: false
date: 2021-06-17 21:49:53
tags: java
---

简单了解一下Shiro。

最近热感冒，生了一场病，持续了一周，现在没啥事了，但是每天感觉好累啊，学习效率极低。

<!--more-->

# 一、概述

Shiro是一款主流的Java安全框架，不依赖任何容器，可以运行在JavaSE和JavaEE项目中。

它的作用是对访问系统的用户进行身份认证、授权、会话管理、加密等操作。

Shiro就是用来解决安全管理的系统化框架。

# 二、Shiro核心组件

核心组件

* UsernamePasswordToken：shiro用来封装用户登录信息，使用用户的登录信息来创建令牌Token

* SecurityManager：Shiro的核心部分，负责安全认证和授权

* Subject：Shiro的一个抽象概念，包含了用户信息。

* Realm：开发者自定义的模块，根据项目的需求，验证和授权的逻辑全部写在Realm中。
* AuthenticationInfo：用户的角色信息集合，认证时使用。
* AuthorzationInfo：角色的权限。
* DefaultWebSecurityManager：安全管理器，开发者自定义的Realm需要注入到DefaultWebSecurityManager进行管理才能生效
* ShiroFilterFactoryBean：过滤器工厂，shiro的基本运行机制是开发者定制规则，shiro去执行。具体的执行操作就是ShiroFilterFactoryBean创建的一个个Filter来完成。

# 三、SpringBoot整合Shiro

## 3.1 环境

pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.5.1</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>demo</name>
    <description>Demo project for Spring Boot</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- https://mvnrepository.com/artifact/org.apache.shiro/shiro-spring -->
        <dependency>
            <groupId>org.apache.shiro</groupId>
            <artifactId>shiro-spring</artifactId>
            <version>1.7.1</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.12</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.1.0</version>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 3.2 自定义shiro

ShiroConfig

```java
@Configuration
public class ShiroConfig {
    @Bean
    public AccountRealm accountRealm(){
        return new AccountRealm();
    }

    @Bean
    public DefaultWebSecurityManager securityManager(@Qualifier("accountRealm") AccountRealm accountRealm){
        return new DefaultWebSecurityManager(accountRealm);
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager securityManager){
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        //权限设置
        Map<String,String> map=new HashMap<>();
        map.put("/main","authc");
        map.put("/manager","perms[manager]");
        map.put("/admin","roles[admin]");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(map);
        //设置登录页面
        shiroFilterFactoryBean.setLoginUrl("/login");
        //设置未授权页面
        shiroFilterFactoryBean.setLoginUrl("/unauth");
        return shiroFilterFactoryBean;
    }
}
```

AccountController

```java
@Controller
public class AccountController {

    @GetMapping("/{url}")
    public String redirect(@PathVariable("url") String url){
        return url;
    }
    @PostMapping("/login")
    public String login(String username, String password, Model model){
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token=new UsernamePasswordToken(username,password);
        try{
            subject.login(token);
            Account account=(Account)subject.getPrincipal();
            subject.getSession().setAttribute("account",account);
            return "index";
        }catch (UnknownAccountException e){
            e.printStackTrace();
            model.addAttribute("msg","用户名错误");
            return "login";
        }catch(IncorrectCredentialsException e){
            e.printStackTrace();
            model.addAttribute("msg","密码错误");
            return "login";
        }
    }
    @GetMapping("/unauth")
    @ResponseBody
    public String unauth(){
        return "未授权、无法访问";
    }
    @GetMapping("/logout")
    public String logout(){
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        return "login";
    }
}
```

Account

```java
@Data
public class Account {
    private Integer id;
    private String username;
    private String password;
    private String perms;
    private String role;
}
```

AccountMapper

```java
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
```

AccountRealm

```java
public class AccountRealm extends AuthorizingRealm {
    @Autowired
    private AccountService accountService;
    /**
     * 授权
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        //获取当期登录的用户信息
        Subject subject = SecurityUtils.getSubject();
        Account account=(Account)subject.getPrincipal();
        //设置角色
        Set<String> roles=new HashSet<>();
        roles.add(account.getRole());
        SimpleAuthorizationInfo info =new SimpleAuthorizationInfo(roles);
        //设置权限
        info.addStringPermission(account.getPerms());
        return info;
    }

    /**
     * 认证
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token=(UsernamePasswordToken)authenticationToken;
        Account account = accountService.findByUsername(token.getUsername());
        if(account!=null){
            return new SimpleAuthenticationInfo(account,account.getPassword(),getName());
        }
        return null;
    }
}
```

AccountServiceImpl

```java
@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountMapper accountMapper;
    @Override
    public Account findByUsername(String username) {
        QueryWrapper queryWrapper=new QueryWrapper();
        queryWrapper.eq("username",username);
        return accountMapper.selectOne(queryWrapper);
    }
}
```

## 3.3 编写认证和授权规则

认证过滤器

1. anon：无需认证
2. authc：必须认证
3. authcBasic：需要通过HttpBasic认证
4. user：不一定通过认证，只要曾经被Shiro记录即可。比如记住密码。

授权过滤器

1. perms：必须拥有某个权限才能访问
2. role：必须拥有某个角色才能访问
3. port：请求的端口必须是指定值才可以
4. rest：请求必须基于RESTful，post、put、get、delete
5. ssl：必须是安全的url请求，协议https

创建三个页面，main.html、manager.html、admin.html

访问权限如下

1. 必须登录才能访问main.html
2. 当前用户必须拥有manager授权才能访问manager.html
3. 当前用户必须拥有admin授权才能访问admin.html

