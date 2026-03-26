package com.inspoverse.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方易支付（Epay）配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.payment.epay")
public class EpayProperties {

  /** 商户ID */
  private String merchantId;

  /** 商户密钥（用于签名） */
  private String merchantKey;

  /** API查询接口地址 */
  private String apiUrl;

  /** 前台提交（收银台）地址 */
  private String submitUrl;

  /** MAPI 接口地址（服务端发起支付） */
  private String mapiUrl;
}
