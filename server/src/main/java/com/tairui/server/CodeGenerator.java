package com.tairui.server;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

/*

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/grid_cabinet_db?useSSL=false&serverTimezone=Asia/Shanghai",
                        "root",
                        "123456")
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("system")
                            .outputDir(System.getProperty("user.dir") + "/server/src/main/java")  // 改为源码根目录
                            .commentDate("yyyy-MM-dd")
                            .dateType(DateType.TIME_PACK)
                            .disableOpenDir();
                })
                // 包配置（修正后）
                .packageConfig(builder -> {
                    builder.parent("com.tairui.server")   // 父包名
                            .entity("entity")             // 实体类包名
                            .mapper("mapper")             // Mapper 接口包名
                            .service("service")           // Service 接口包名
                            .serviceImpl("service.impl")  // Service 实现包名
                            .xml("mapper.xml")            // XML 映射文件包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml,
                                    System.getProperty("user.dir") + "/server/src/main/resources/mapper"));
                })
                // 策略配置（增加 serviceBuilder）
                .strategyConfig(builder -> {
                    builder.addInclude("system_config")
                            // Entity 策略
                            .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .idType(IdType.AUTO)
                            // Mapper 策略
                            .mapperBuilder()
                            .enableBaseResultMap()
                            .enableMapperAnnotation()
                            // Service 策略（显式配置）
                            .serviceBuilder()
                            .formatServiceFileName("%sService")          // 接口名：CabinetConfigService
                            .formatServiceImplFileName("%sServiceImpl")  // 实现类名：CabinetConfigServiceImpl
                    // Controller 策略（可选，如需要可取消注释）
                    // .controllerBuilder()
                    // .enableRestStyle()
                    ;
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
*/
