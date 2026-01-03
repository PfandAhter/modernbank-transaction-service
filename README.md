# ModernBank Transaction Service - Teknik Proje Raporu

**Hazırlayan:** Ataberk BAKIR
**Tarih:** 01 Ocak 2026  
**Versiyon:** 1.0

---

## Özet (Executive Summary)

Bu rapor, ModernBank Transaction Service mikroservisinin kapsamlı bir teknik analizini sunmaktadır. Proje, modern bankacılık sistemlerinde finansal işlemlerin güvenli ve ölçeklenebilir bir şekilde yönetilmesi için tasarlanmış bir Spring Boot 3.2 tabanlı mikroservistir. Sistem, makine öğrenmesi destekli dolandırıcılık tespiti, event-driven mimari ve circuit breaker pattern gibi kurumsal düzeyde özellikleri içermektedir.

---

## 1. Giriş ve Proje Kapsamı

### 1.1 Projenin Amacı

Transaction Service, modern bir dijital bankacılık platformunun çekirdek finansal işlem yönetim katmanını oluşturmaktadır. Temel sorumlulukları:

- **Para Transferi İşlemleri:** Hesaplar arası EFT/havale işlemlerinin yönetimi
- **Para Çekme/Yatırma:** Müşteri hesaplarından nakit işlemleri
- **ATM Entegrasyonu:** ATM kanalı üzerinden gerçekleştirilen işlemler
- **Dolandırıcılık Tespiti:** ML tabanlı gerçek zamanlı risk analizi
- **İşlem Geçmişi:** Müşteri işlem hareketlerinin sorgulanması

### 1.2 Kullanılan Teknolojiler

| Kategori | Teknoloji | Versiyon | Kullanım Amacı |
|----------|-----------|----------|----------------|
| Framework | Spring Boot | 3.2.0 | Uygulama altyapısı |
| Veritabanı | MySQL | - | Kalıcı veri depolama |
| Önbellek | Redis | - | Oturum ve geçici veri yönetimi |
| Mesajlaşma | Apache Kafka | - | Event-driven iletişim |
| HTTP İstemci | OpenFeign | 3.0.2 | Mikroservis iletişimi |
| Dayanıklılık | Resilience4j | 2.2.0 | Circuit Breaker pattern |
| Metrikler | Micrometer + Prometheus | - | Sistem izleme |
| Build | Maven | - | Bağımlılık yönetimi |

---

## 2. Sistem Mimarisi

### 2.1 Genel Mimari Yapı

Transaction Service, mikroservis mimarisi içinde merkezi bir konumda yer almaktadır. Event-driven mimari prensiplerine göre tasarlanmış olup, asenkron iletişim için Apache Kafka kullanmaktadır.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ModernBank Ecosystem                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐     ┌──────────────────────────────────────────────────┐ │
│   │   Frontend   │────▶│           Transaction Service                    │ │
│   │   (Mobile/   │     │  ┌─────────────────────────────────────────────┐ │ │
│   │    Web)      │     │  │              REST Controllers               │ │ │
│   └──────────────┘     │  │  • TransactionServiceController             │ │ │
│                        │  │  • FraudServiceController                   │ │ │
│                        │  │  • TransactionAnalyzeController             │ │ │
│                        │  └─────────────────────────────────────────────┘ │ │
│                        │                        │                          │ │
│                        │  ┌─────────────────────────────────────────────┐ │ │
│                        │  │            Service Layer                     │ │ │
│                        │  │  • FraudEvaluationService                   │ │ │
│                        │  │  • FraudDecisionEngine                      │ │ │
│                        │  │  • TransactionService                       │ │ │
│                        │  └─────────────────────────────────────────────┘ │ │
│                        │                        │                          │ │
│                        │  ┌─────────────────────────────────────────────┐ │ │
│                        │  │           Kafka Integration                  │ │ │
│                        │  │  Producers:          │  Consumers:          │ │ │
│                        │  │  • TransferMoney     │  • TransactionConsumer│ │ │
│                        │  │  • WithdrawATM       │  • NotificationConsumer│ │ │
│                        │  │                      │  • InvoiceConsumer    │ │ │
│                        │  └─────────────────────────────────────────────┘ │ │
│                        └──────────────────────────────────────────────────┘ │
│                                          │                                   │
│              ┌───────────────────────────┼───────────────────────────┐      │
│              │                           │                           │      │
│              ▼                           ▼                           ▼      │
│   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐     │
│   │  Account Service │    │  Fraud ML Service│    │Notification Svc  │     │
│   │  (Feign Client)  │    │  (Feign Client)  │    │  (Feign Client)  │     │
│   └──────────────────┘    └──────────────────┘    └──────────────────┘     │
│                                                                              │
│   ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐     │
│   │      MySQL       │    │      Redis       │    │  Invoice Service │     │
│   │   (Persistence)  │    │    (Caching)     │    │  (Feign Client)  │     │
│   └──────────────────┘    └──────────────────┘    └──────────────────┘     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Katmanlı Mimari

Proje, klasik Spring Boot katmanlı mimarisini uygulamaktadır:

1. **Controller Katmanı:** REST API endpoint'leri
2. **Service Katmanı:** İş mantığı ve orkestrasyon
3. **Repository Katmanı:** Veri erişim işlemleri
4. **Entity Katmanı:** Veritabanı modelleri
5. **Event Katmanı:** Kafka producer/consumer bileşenleri
6. **Client Katmanı:** Harici servis entegrasyonları (Feign)

---

## 3. Veri Modeli

### 3.1 Transaction Entity

Sistemin temel veri modelini oluşturan `Transaction` entity'si, bir finansal işlemin tüm bilgilerini içermektedir:

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    // Temel Kimlik
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    // İşlem Bilgileri
    private String accountId;
    private Double amount;
    private Currency currency;
    private TransactionType type;
    private TransactionChannel channel;
    private TransactionCategory category;
    private TransactionStatus status;
    
    // Gönderen/Alıcı Bilgileri
    private String senderFirstName, senderSecondName, senderLastName;
    private String receiverFirstName, receiverSecondName, receiverLastName;
    private String receiverIban, receiverTckn;
    
    // Dolandırıcılık Değerlendirme Alanları
    private Double riskScore;
    private RiskLevel riskLevel;
    private FraudDecision fraudDecision;
    private LocalDateTime fraudEvaluatedAt;
    private String fraudDecisionReason;
}
```

### 3.2 FraudDecision Entity

ML tabanlı dolandırıcılık sinyallerini ve karar sonuçlarını saklayan entity:

```java
@Entity
@Table(name = "fraud_decisions")
public class FraudDecision {
    private String transactionId;
    private String pendingTransactionId;
    
    // ML Sinyalleri (Kredi skorlaması için zorunlu)
    private Double riskScore;
    private RiskLevel riskLevel;
    private Double amountToBalanceRatio;
    private Boolean newReceiverFlag;
    
    // Karar Sonuçları
    private FraudDecisionAction decisionTaken;
    private String confirmationResult; // USER_CONFIRMED, TIMEOUT, BLOCKED
    private Long timeToConfirm; // milisaniye
}
```

### 3.3 FraudEvaluation Entity

Makine öğrenmesi değerlendirme sonuçlarını saklayan entity:

```java
@Entity
@Table(name = "fraud_evaluation")
public class FraudEvaluation {
    private String transactionId;
    private String userId;
    private Double riskScore;
    private RiskLevel riskLevel;
    private FraudDecisionAction recommendedAction;
    
    @Column(columnDefinition = "json")
    private String featureVector;      // Öznitelik vektörü (JSON)
    
    @Column(columnDefinition = "json")
    private String featureImportance;  // Öznitelik önemliliği (JSON)
    
    private String modelVersion;
}
```

---

## 4. Dolandırıcılık Tespit Sistemi

### 4.1 Genel Bakış

Sistem, her finansal işlem için gerçek zamanlı dolandırıcılık risk değerlendirmesi yapmaktadır. Bu değerlendirme, harici bir ML servisi ile senkron iletişim kurarak gerçekleştirilmektedir.

### 4.2 Risk Seviyesi Sınıflandırması

| Risk Seviyesi | Skor Aralığı | Karar |
|---------------|--------------|-------|
| **LOW** | < 0.30 | APPROVE - İşlem onaylanır |
| **MEDIUM** | 0.30 - 0.70 | HOLD - Kullanıcı onayı beklenir |
| **HIGH** | > 0.70 | BLOCK - İşlem reddedilir |

### 4.3 Öznitelik Vektörü (Feature Vector)

Dolandırıcılık tespiti için kullanılan öznitelikler:

| Öznitelik | Açıklama | Hesaplama |
|-----------|----------|-----------|
| `amountToAvgRatio` | İşlem tutarının son 7 günlük ortalamaya oranı | `amount / avgTransactionAmount7d` |
| `balanceDrainRatio` | İşlem tutarının hesap bakiyesine oranı | `amount / accountBalance` |
| `velocity24h` | Son 24 saatteki işlem sayısı | `count(transactions)` |
| `velocity7d` | Son 7 gündeki işlem sayısı | `count(transactions)` |
| `cardAgeMonths` | Kartın yaşı (ay) | `cardAgeMonths` |
| `isNewReceiver` | Yeni alıcı mı? | `boolean (1.0/0.0)` |
| `isWeekend` | Hafta sonu mu? | `boolean (1.0/0.0)` |
| `previousFraudFlag` | Geçmişte dolandırıcılık var mı? | `boolean (1.0/0.0)` |
| `isOffHours` | Mesai dışı saat mi? | `hour < 6 || hour > 22` |

### 4.4 Karar Motoru (Decision Engine)

```java
public interface FraudDecisionEngine {
    /**
     * Karar Matrisi:
     * - LOW risk (< 0.30) → APPROVE
     * - MEDIUM risk (0.30 - 0.70) → HOLD + Bildirim
     * - HIGH risk (ilk kez) → HOLD_STRONG_AUTH
     * - HIGH risk (24 saatte ≥2) → BLOCK
     * - HIGH + önceki onaylanmış dolandırıcılık → BLOCK
     * - Kara listedeki alıcı → BLOCK
     */
    FraudDecision evaluate(
        FraudCheckResponse mlResponse,
        AccountProfileResponse accountProfile,
        String receiverIban,
        String pendingTransactionId,
        Double amount
    );
}
```

### 4.5 Temel Prensipler

> [!IMPORTANT]
> **Değişmez Kurallar:**
> 1. **AI önerir, iş kuralları karar verir** - ML servisi sadece risk skoru sağlar
> 2. **Risk çözülmeden para hareket etmez** - HOLD durumunda işlem bekletilir
> 3. **İlk yüksek risk = HOLD, tekrar = BLOCK** - Kademeli güvenlik yaklaşımı

---

## 5. Event-Driven Mimari

### 5.1 Kafka Entegrasyonu

Sistem, Apache Kafka kullanarak asenkron event-driven mimari uygulamaktadır.

**Producer'lar:**
- `ITransactionServiceProducer`: Para çekme, yatırma ve transfer işlemleri
- `IWithdrawFromATMServiceProducer`: ATM işlemleri

**Consumer'lar:**
- `TransactionServiceConsumer`: Ana işlem tüketicisi (811 satır kod)
- `NotificationServiceConsumer`: Bildirim işleme
- `InvoiceServiceConsumer`: Fatura oluşturma
- `WithdrawFromATMServiceConsumer`: ATM işlem tüketicisi

### 5.2 İşlem Akışı

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Para Transfer İşlem Akışı                         │
└─────────────────────────────────────────────────────────────────────┘

[Client Request]
       │
       ▼
┌──────────────────┐
│ REST Controller  │ ◀── Validation + Idempotency Check
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Fraud Evaluation │ ◀── Senkron ML Servisi Çağrısı
│     Service      │
└────────┬─────────┘
         │
    ┌────┴────┬──────────┐
    ▼         ▼          ▼
 [APPROVE]  [HOLD]    [BLOCK]
    │         │          │
    ▼         ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐
│ Kafka  │ │Pending │ │ Reject │
│Producer│ │  TX DB │ │Response│
└────┬───┘ └────────┘ └────────┘
     │
     ▼
┌──────────────────┐
│ Kafka Consumer   │
│ (TransactionSvc) │
└────────┬─────────┘
         │
    ┌────┴────┬─────────────┐
    ▼         ▼             ▼
[Update    [Update      [Generate
Balance]   TX Record]    Invoice]
    │         │             │
    ▼         ▼             ▼
┌────────┐ ┌────────┐  ┌────────┐
│Account │ │ MySQL  │  │Invoice │
│Service │ │   DB   │  │Service │
└────────┘ └────────┘  └────────┘
```

---

## 6. Harici Servis Entegrasyonları

### 6.1 Feign Client'lar

Sistem, Spring Cloud OpenFeign kullanarak diğer mikroservislerle iletişim kurmaktadır:

| Client | Servis | Temel Fonksiyonlar |
|--------|--------|-------------------|
| `AccountServiceClient` | Account Service | Hesap sorgulama, bakiye güncelleme, kara liste kontrolü |
| `FraudMLServiceClient` | Fraud ML Service | Dolandırıcılık risk değerlendirmesi |
| `NotificationServiceClient` | Notification Service | Push bildirim gönderme |
| `InvoiceServiceClient` | Invoice Service | Dekont/fatura oluşturma |
| `ParameterServiceClient` | Parameter Service | Sistem parametreleri |
| `ATMReportingServiceClient` | ATM Reporting | ATM işlem raporlama |

### 6.2 Circuit Breaker Pattern

Resilience4j kütüphanesi kullanılarak circuit breaker pattern uygulanmaktadır:

```java
@Service
public class ResilientFraudMLService {
    // Circuit breaker ile sarmalanmış ML servisi çağrısı
    // Fallback mekanizması ile servis kesintilerinde güvenli işlem
}
```

> [!TIP]
> Circuit breaker açıldığında (ML servisi erişilemez), sistem **fallback** modu devreye girer ve işlemler **LOW risk** olarak değerlendirilir.

---

## 7. REST API Endpoint'leri

### 7.1 Transaction Controller

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/api/v1/transaction/withdraw` | Para çekme |
| POST | `/api/v1/transaction/deposit` | Para yatırma |
| POST | `/api/v1/transaction/transfer` | Para transferi |
| POST | `/api/v1/transaction/atm/deposit` | ATM'ye para yatırma |
| POST | `/api/v1/transaction/atm/withdraw` | ATM'den para çekme |
| GET | `/api/v1/transaction/all` | İşlem geçmişi sorgulama |

### 7.2 Fraud Controller

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| POST | `/api/v1/fraud/confirm` | Dolandırıcılık onayı (analist/müşteri) |
| POST | `/api/v1/fraud/confirm-legitimate/{id}` | Meşru işlem onayı (false positive) |
| POST | `/api/v1/fraud/additional-approval` | Ek onay talebi |

### 7.3 Transaction Analyze Controller

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/v1/transactions/analyze` | İşlem analizi (periyodik raporlama) |

---

## 8. Güvenlik ve Doğrulama

### 8.1 Idempotency

Sistem, `@Idempotent` annotation'ı ile tekrarlanan isteklerin güvenli işlenmesini sağlamaktadır:

```java
@Override
@Idempotent  // Tekrarlayan istekleri güvenle işle
public ResponseEntity<BaseResponse> transferMoney(TransferMoneyRequest request) {
    // ...
}
```

### 8.2 Validation

İşlem limitleri ve kuralları için doğrulama katmanı:

- Günlük para çekme limiti kontrolü
- Para yatırma limiti kontrolü
- ATM işlem limiti kontrolü
- Yeterli bakiye kontrolü
- Hesap sahipliği doğrulaması

---

## 9. İzleme ve Gözlemlenebilirlik

### 9.1 Actuator Endpoint'leri

Spring Boot Actuator ile sistem sağlık durumu izlenmektedir:

- `/actuator/health` - Sağlık durumu
- `/actuator/metrics` - Metrikler
- `/actuator/prometheus` - Prometheus formatında metrikler

### 9.2 Prometheus Entegrasyonu

Micrometer kütüphanesi ile Prometheus'a metrik aktarımı yapılmaktadır.

---

## 10. Sonuç ve Değerlendirme

### 10.1 Güçlü Yönler

1. **Ölçeklenebilir Mimari:** Event-driven yaklaşım ile yatay ölçeklenebilirlik
2. **Güvenlik Odaklı:** ML tabanlı gerçek zamanlı dolandırıcılık tespiti
3. **Dayanıklılık:** Circuit breaker pattern ile hata toleransı
4. **Gözlemlenebilirlik:** Prometheus/Actuator ile kapsamlı izleme
5. **Modüler Tasarım:** Temiz katmanlı mimari

### 10.2 Teknik Özellikler Özeti

| Özellik | Detay |
|---------|-------|
| Toplam Kaynak Dosya | ~143 Java dosyası |
| Java Versiyonu | 17 |
| Veritabanı | MySQL |
| Önbellek | Redis |
| Mesajlaşma | Apache Kafka |
| Konteynerizasyon | Docker (Dockerfile mevcut) |

### 10.3 Proje Durumu

Bu Transaction Service, kurumsal düzeyde bir bankacılık mikroservisi standartlarını karşılamaktadır. ML entegrasyonu, event-driven mimari ve dayanıklılık pattern'leri ile modern fintech uygulamalarının gereksinimlerini sağlamaktadır.

---

## Ekler

### Ek A: Enum Değerleri

**TransactionStatus:** `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`, `ON_HOLD`, `BLOCKED`

**RiskLevel:** `LOW`, `MEDIUM`, `HIGH`

**FraudDecision:** `APPROVE`, `HOLD`, `BLOCK`

**TransactionType:** `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`

**TransactionChannel:** `MOBILE`, `WEB`, `ATM`, `BRANCH`

---
