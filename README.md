# 🍽️ Banco de Alimentos (BARC)

> **Sistema completo de doações de alimentos** com autenticação robusta, pagamentos PIX, upload de imagens e painel administrativo

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🎯 Sobre o Projeto

**BARC (Banco de Alimentos)** é uma aplicação Android completa para gerenciamento de doações de alimentos, conectando doadores a instituições beneficentes através de um sistema moderno e escalável.

O projeto implementa soluções avançadas para **autenticação multi-método**, **processamento de pagamentos PIX com QR Code dinâmico**, **upload de imagens para CDN**, e **painel administrativo completo** - demonstrando conhecimentos sólidos em desenvolvimento mobile e arquitetura de sistemas.

### 💡 Problema Resolvido

Facilitar o processo de doação de alimentos através de uma plataforma digital que:
- Permite doações monetárias para compra de alimentos
- Conecta pontos de coleta com doadores
- Gerencia estoque de produtos disponíveis
- Processa pagamentos de forma segura via PIX

---

## ✨ Funcionalidades Principais

### 🔐 Autenticação Avançada
- **Login com Email/Senha** via Firebase Authentication
- **Google Sign-In (OAuth 2.0)** com arquitetura de ponte personalizada
- **Verificação de email obrigatória** com deep linking
- **Recuperação de senha** via email action links
- **Atualização de email** com reautenticação automática
- **Sistema de roles** (usuário comum / administrador)

### 💰 Sistema de Pagamentos PIX
- **Geração dinâmica de QR Code PIX** usando protocolo BR Code
- **Integração com Mercado Pago API** via webhooks
- **Countdown timer** de 10 minutos para expiração
- **Listener em tempo real** para confirmação de pagamento
- **Cópia de chave PIX** para área de transferência
- **Histórico de doações** do usuário

### 🛒 Carrinho de Compras
- **Adicionar/remover produtos** com persistência local (SharedPreferences)
- **Cálculo automático de totais** com formatação monetária
- **Sincronização com Firebase** ao finalizar pedido
- **Validação de estoque** em tempo real

### 📦 Gerenciamento de Produtos (Admin)
- **CRUD completo** de produtos
- **Upload de imagens para Cloudinary** (CDN)
- **Edição de preços e quantidades**
- **Sincronização em tempo real** via Firestore

### 📍 Pontos de Coleta
- **Mapa com localização geográfica** (Google Maps integration)
- **CRUD de pontos de coleta** (admin)
- **Informações de contato e horários**
- **Cálculo de distância** do usuário

### 👥 Painel Administrativo
- **Dashboard com métricas** de doações
- **Gerenciamento de admins** (adicionar/remover)
- **Visualização de pedidos** em tempo real
- **Aprovação de doações**
- **Configurações de PIX e WhatsApp** para pagamentos

---

## 🏗️ Arquitetura e Tecnologias

### Backend & Database
```
Firebase Authentication     → Gerenciamento de usuários e sessões
Firebase Firestore          → Database NoSQL em tempo real
Firebase Cloud Functions    → Webhooks e processamento serverless
Vercel Functions            → API de pagamentos PIX
Cloudinary                  → CDN para imagens de produtos
```

### Integrações Externas
```
Google Sign-In API          → OAuth 2.0 authentication
Mercado Pago API            → Processamento de pagamentos PIX
Google Maps API             → Geolocalização de pontos de coleta
ZXing (Zebra Crossing)      → Geração de QR Codes
OkHttp                      → Cliente HTTP para APIs
```

### Padrões e Arquitetura
```
MVC (Model-View-Controller) → Separação de responsabilidades
Singleton Pattern           → Gerenciamento de instâncias Firebase
Repository Pattern          → Abstração de acesso a dados
Observer Pattern            → Listeners em tempo real (Firestore)
Activity Result API         → Comunicação entre Activities
Deep Linking                → Navegação via URLs externas
```

### Recursos Avançados Implementados

#### 🔥 Autenticação Robusta
- **AuthBridgeActivity**: Activity dedicada ao fluxo Google Sign-In que resolve problemas de lifecycle durante autenticação OAuth
- **AuthGate**: Sistema de verificação de autenticação com logging detalhado
- **Tratamento de edge cases**: Re-autenticação para operações sensíveis, verificação de email obrigatória

#### 💳 Sistema PIX Completo
```java
// Geração de payload PIX usando BR Code
PixPayloadBuilder.build(pixKey, amount, merchantName, city)
  
// Countdown timer com Handler
Handler + Runnable → Atualização de UI a cada segundo
  
// Listener Firestore para confirmação
db.collection("pagamentos").document(id)
  .addSnapshotListener() → Navegação automática ao confirmar
```

#### 🖼️ Upload de Imagens
```java
// CloudinaryUploader customizado (sem dependências extras)
CloudinaryUploader.upload(context, imageUri, cloudName, preset,
  new Callback() {
    onSuccess(secureUrl) → Salvar URL no Firestore
    onError(message) → Retry logic implementado
  }
)
```

#### 🎨 UI/UX Moderna
- **Material Design 3** components
- **Edge-to-Edge** rendering (Android 15+)
- **WindowInsets** handling para telas modernas
- **Splash Screen** com roteamento inteligente
- **Progress indicators** durante operações assíncronas
- **Validação de formulários** em tempo real

---

## 📁 Estrutura do Projeto

```
app/src/main/
├── java/com/instituto/bancodealimentos/
│   ├── activities/
│   │   ├── MainActivity.java              # Tela de boas-vindas
│   │   ├── SplashActivity.java            # Roteamento inicial (user/admin)
│   │   ├── telalogin.java                 # Login email + Google
│   │   ├── telaregistro.java              # Registro de usuários
│   │   ├── menu.java                      # Menu principal (usuário)
│   │   ├── menu_admin.java                # Menu administrativo
│   │   └── AuthBridgeActivity.java        # Ponte para Google Sign-In
│   │
│   ├── authentication/
│   │   ├── AuthGate.java                  # Verificação de sessão
│   │   ├── EsqueciSenhaActivity.java      # Recuperação de senha
│   │   ├── NovoEmailActivity.java         # Atualização de email
│   │   └── EmailActionRouterActivity.java # Deep link handler
│   │
│   ├── shopping/
│   │   ├── carrinho.java                  # Carrinho de compras
│   │   ├── pagamento.java                 # Tela de pagamento PIX
│   │   ├── pedido_pago.java               # Confirmação de pagamento
│   │   └── HistoricoDoacoesActivity.java  # Histórico do usuário
│   │
│   ├── admin/
│   │   ├── admin_produtos.java            # Gerenciamento de produtos
│   │   ├── criar_produto.java             # Cadastro de produtos
│   │   ├── editar_produto.java            # Edição de produtos
│   │   ├── gerenciar_admins.java          # CRUD de administradores
│   │   ├── pontosdecoleta_admin.java      # Gerenciamento de pontos
│   │   └── AdminDoacoesActivity.java      # Dashboard de doações
│   │
│   ├── models/
│   │   ├── Produto.java                   # Model de produto
│   │   ├── Doacao.java                    # Model de doação
│   │   ├── PontoColeta.java               # Model de ponto de coleta
│   │   └── AdminUser.java                 # Model de administrador
│   │
│   ├── adapters/
│   │   ├── ProdutoAdapter.java            # RecyclerView de produtos
│   │   ├── CarrinhoAdapter.java           # RecyclerView do carrinho
│   │   ├── DoacaoAdapter.java             # RecyclerView de doações
│   │   └── PontoColetaAdapter.java        # RecyclerView de pontos
│   │
│   ├── utils/
│   │   ├── CloudinaryUploader.java        # Upload de imagens
│   │   ├── PixPayloadBuilder.java         # Geração de payload PIX
│   │   ├── WindowInsetsHelper.java        # Edge-to-edge support
│   │   ├── SettingsRepository.java        # Persistência local
│   │   ├── Money.java                     # Formatação monetária
│   │   └── Retry.java                     # Retry logic para APIs
│   │
│   └── BDAApp.java                        # Application class (init)
│
└── res/
    ├── layout/                            # 46 layouts XML
    ├── values/
    │   ├── colors.xml                     # Paleta de cores
    │   ├── strings.xml                    # Textos do app
    │   └── themes.xml                     # Temas Material Design
    └── drawable/                          # Assets e ícones
```

---

## 🚀 Como Executar o Projeto

### Pré-requisitos

- **Android Studio** Hedgehog ou superior
- **JDK 17** ou superior
- **Android SDK 34** (API Level 34)
- **Conta Google** (para autenticação)
- **Projeto Firebase** configurado

### Configuração do Firebase

1. **Crie um projeto no [Firebase Console](https://console.firebase.google.com/)**

2. **Adicione um app Android:**
   - Package name: `com.instituto.bancodealimentos`
   - Baixe o arquivo `google-services.json`
   - Coloque em `app/google-services.json`

3. **Ative os serviços:**
   ```
   Authentication → Sign-in method:
   ✓ Email/Password
   ✓ Google
   
   Firestore Database → Create database (modo produção)
   
   Authentication → Settings → Authorized domains:
   ✓ albertoschneider.github.io (para deep links)
   ```

4. **Configure SHA-1/SHA-256:**
   ```bash
   # Debug keystore
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android
   
   # Adicione os fingerprints SHA no Firebase Console
   ```

### Configuração do Google Sign-In

1. **No Firebase Console:**
   - Authentication → Sign-in method → Google → Enable
   - Baixe o `Web client ID`

2. **No projeto:**
   - Verifique se `google-services.json` contém o `client_id`
   - O app usa `R.string.default_web_client_id` automaticamente

### Configuração do Cloudinary (Upload de Imagens)

1. **Crie uma conta em [Cloudinary](https://cloudinary.com/)**

2. **Crie um upload preset:**
   - Settings → Upload → Upload presets
   - Add upload preset → Unsigned
   - Nome: `imagensBARC`

3. **Atualize no código (opcional):**
   ```java
   // CloudinaryUploader já está configurado com:
   cloudName = "dobs6lmfz"
   uploadPreset = "imagensBARC"
   ```

### Configuração de Pagamentos PIX

**Importante:** O sistema de pagamentos requer configuração de webhooks no Mercado Pago e deploy da API em Vercel. Para ambiente de testes, o app funciona sem essa configuração (pagamentos não serão confirmados automaticamente).

### Executar o App

```bash
# 1. Clone o repositório
git clone https://github.com/albertoschneider/Banco-De-Alimentos.git
cd Banco-De-Alimentos

# 2. Abra no Android Studio
# File → Open → Selecione a pasta do projeto

# 3. Aguarde sincronização do Gradle

# 4. Adicione google-services.json em app/

# 5. Build → Rebuild Project

# 6. Conecte dispositivo ou inicie emulador

# 7. Run → Run 'app'
```

### Primeiro Login

1. **Registro:** Crie uma conta com email/senha
2. **Verificação:** Clique no link enviado ao email
3. **Login:** Entre com as credenciais ou Google Sign-In
4. **Explorar:** Navegue pelo app como usuário comum

### Criar Conta Admin

```bash
# No Firebase Console:
# Firestore Database → Coleção "admins" → Add document

Document ID: [UID do usuário]
Campos:
  - email: "seu@email.com"
  - nome: "Seu Nome"
  - criadoEm: [Timestamp atual]
```

Faça logout e login novamente para acessar o painel administrativo.

---

## 🔧 Tecnologias e Dependências

### Core Android
```gradle
minSdk = 24                    // Android 7.0 (94% dos dispositivos)
targetSdk = 34                 // Android 14
compileSdk = 34
```

### Principais Bibliotecas

```gradle
// Firebase
com.google.firebase:firebase-auth:22.3.1
com.google.firebase:firebase-firestore:24.10.1
com.google.firebase:firebase-storage:20.3.0

// Google Sign-In
com.google.android.gms:play-services-auth:20.7.0

// Material Design
com.google.android.material:material:1.11.0

// Networking
com.squareup.okhttp3:okhttp:4.12.0
com.google.code.gson:gson:2.10.1

// QR Code
com.google.zxing:core:3.5.2

// Maps (opcional)
com.google.android.gms:play-services-maps:18.2.0
com.google.android.gms:play-services-location:21.1.0
```

---

## 📊 Fluxos Principais

### Fluxo de Autenticação

```
SplashActivity
    ↓
    Usuário logado? 
    ├─ NÃO → MainActivity → telalogin
    │                         ↓
    │                      Email/Senha ou Google
    │                         ↓
    │                   (Google) AuthBridgeActivity
    │                         ↓
    │                   FirebaseAuth.signIn()
    │                         ↓
    └─ SIM → É admin?
             ├─ SIM → menu_admin
             └─ NÃO → menu
```

### Fluxo de Pagamento PIX

```
carrinho.java
    ↓
Finalizar Pedido
    ↓
pagamento.java
    ├─ Gerar payload PIX (BR Code)
    ├─ Criar QR Code (ZXing)
    ├─ Salvar em Firestore/pagamentos
    ├─ Iniciar countdown (10min)
    └─ Listener em tempo real
        ↓
    Pagamento confirmado?
        ├─ SIM → pedido_pago.java
        │         ↓
        │     Salvar em /doacoes
        │         ↓
        │     Limpar carrinho
        │         ↓
        │     HistoricoDoacoesActivity
        │
        └─ NÃO → Aguardar ou expirar
```

### Fluxo de Upload de Imagem

```
criar_produto.java
    ↓
Selecionar imagem (Intent)
    ↓
CloudinaryUploader.upload()
    ├─ Thread background
    ├─ Multipart form-data
    ├─ POST api.cloudinary.com
    └─ Callback (UI thread)
        ↓
    secure_url recebida
        ↓
    Salvar em Firestore/produtos
        ↓
    RecyclerView atualizado (listener)
```

---

## 🎓 Desafios Técnicos Resolvidos

### 1. Google Sign-In com Activity Lifecycle
**Problema:** Activity sendo destruída durante o fluxo OAuth causava perda de resultado.

**Solução:** Criação de `AuthBridgeActivity` dedicada + `ActivityResultLauncher` + flags de proteção contra destruição prematura.

```java
// Proteção contra recriação durante sign-in
if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
    finish();
    return;
}
```

### 2. Verificação de Email Obrigatória
**Problema:** Usuários logavam sem verificar email.

**Solução:** Deep linking com `ActionCodeSettings` + handler de URL customizado.

```java
ActionCodeSettings settings = ActionCodeSettings.newBuilder()
    .setUrl("https://albertoschneider.github.io/success/email-verificado/")
    .setHandleCodeInApp(true)
    .setAndroidPackageName("com.instituto.bancodealimentos", true, null)
    .build();
```

### 3. Pagamento PIX Sem SDK
**Problema:** SDKs de pagamento são pesados e complexos.

**Solução:** Implementação manual do protocolo BR Code + integração com webhooks Mercado Pago.

```java
// Payload PIX seguindo especificação BR Code (BACEN)
String payload = PixPayloadBuilder.build(
    chavePix,           // 26 (Chave PIX)
    valorEmCentavos,    // 54 (Valor da transação)
    nomeBeneficiario,   // 59 (Nome do beneficiário)
    cidade              // 60 (Cidade)
);
```

### 4. Sincronização de Carrinho
**Problema:** Manter carrinho entre sessões sem backend complexo.

**Solução:** Persistência local com `SharedPreferences` + serialização JSON via Gson.

```java
// Salvar carrinho
String json = new Gson().toJson(produtos);
prefs.edit().putString(KEY_CART, json).apply();

// Recuperar carrinho
String json = prefs.getString(KEY_CART, "[]");
Type type = new TypeToken<ArrayList<Produto>>(){}.getType();
produtos = new Gson().fromJson(json, type);
```

### 5. Upload sem OkHttp Multipart
**Problema:** Evitar dependências desnecessárias para upload de imagem.

**Solução:** Implementação manual de multipart/form-data com `HttpURLConnection`.

```java
// Construção manual do boundary e corpo da requisição
String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();
conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
// ... código de montagem do body
```

---

## 🔐 Segurança

### Boas Práticas Implementadas

✅ **Autenticação Firebase** - Tokens JWT gerenciados automaticamente  
✅ **Rules Firestore** - Controle de acesso por role (user/admin)  
✅ **HTTPS obrigatório** - Todas as comunicações criptografadas  
✅ **Validação de inputs** - Prevenção de SQL injection e XSS  
✅ **Email verification** - Confirmação obrigatória de conta  
✅ **Re-authentication** - Para operações sensíveis (trocar email/senha)  
✅ **google-services.json** - Nunca commitado (`.gitignore`)  
✅ **API keys** - Restritas por package name e SHA-1  

### Firestore Security Rules (Exemplo)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Produtos: leitura pública, escrita apenas admin
    match /produtos/{produtoId} {
      allow read: if true;
      allow write: if request.auth != null && 
        exists(/databases/$(database)/documents/admins/$(request.auth.uid));
    }
    
    // Doações: usuário vê apenas as próprias
    match /doacoes/{doacaoId} {
      allow read: if request.auth != null && 
        request.auth.uid == resource.data.usuarioId;
      allow create: if request.auth != null;
    }
    
    // Admins: apenas outros admins podem modificar
    match /admins/{adminId} {
      allow read: if request.auth != null &&
        exists(/databases/$(database)/documents/admins/$(request.auth.uid));
      allow write: if request.auth != null &&
        exists(/databases/$(database)/documents/admins/$(request.auth.uid));
    }
  }
}
```

---

## 🤝 Contribuições

Este projeto foi desenvolvido como trabalho acadêmico para o curso Técnico em Informática. Contribuições, sugestões e feedback são bem-vindos!

### 👥 Equipe de Desenvolvimento

**Projeto desenvolvido colaborativamente como trabalho acadêmico:**

- **Alberto Schneider** - Tech Lead & Developer
  - Arquitetura e estrutura do projeto
  - Implementação de autenticação (Firebase Auth, Google Sign-In)
  - Sistema de pagamentos PIX
  - Integração Firebase Firestore
  - Upload de imagens (Cloudinary)
  - Painel administrativo
  
- **Bernardo Fritzen Siegle** - Developer
  - Suporte no desenvolvimento
  - Testes e validação de funcionalidades
  
- **Lorenzo Panigo** - Developer  
  - Suporte no desenvolvimento
  - Testes e validação de funcionalidades

### Como Contribuir

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Add: Nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

### Padrões de Commit

```
Add: Nova funcionalidade
Fix: Correção de bug
Update: Atualização de código existente
Refactor: Refatoração sem mudança de comportamento
Docs: Apenas documentação
Style: Formatação, ponto e vírgula, etc
Test: Adição de testes
```

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 📞 Contato

**Alberto Schneider**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Alberto_Schneider-0077B5?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/alberto-schneider/)
[![Email](https://img.shields.io/badge/Email-albertoschneider891@gmail.com-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertoschneider891@gmail.com)
[![GitHub](https://img.shields.io/badge/GitHub-albertoschneider-181717?style=for-the-badge&logo=github)](https://github.com/albertoschneider)

📱 WhatsApp: +55 51 99159-1769

---

## 🎓 Contexto Acadêmico

Projeto desenvolvido como parte do curso **Técnico em Informática** no **Instituto Ivoti** (2023-2026).

**Objetivo educacional:** Aplicar conhecimentos de desenvolvimento mobile Android, integração com serviços cloud, arquitetura de software e boas práticas de programação em um projeto real e funcional.

---

## 🏆 Conquistas do Projeto

✨ **62 arquivos Java** com código limpo e documentado  
✨ **46 layouts XML** com Material Design  
✨ **140+ commits** mostrando evolução do projeto  
✨ **Arquitetura escalável** pronta para crescimento  
✨ **Autenticação robusta** com múltiplos métodos  
✨ **Pagamentos reais** via PIX integrado  
✨ **Upload de imagens** para CDN  
✨ **Painel admin completo** para gerenciamento  
✨ **Logs detalhados** para debugging  
✨ **Tratamento de erros** em todos os fluxos críticos  

---

<div align="center">

### 🌟 Desenvolvido com dedicação, código limpo e muita determinação 🌟

**Se este projeto te ajudou ou te inspirou, considere dar uma ⭐ no repositório!**

</div>
