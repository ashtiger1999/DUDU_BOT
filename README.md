# 🤖 두두봇

Discord 서버에서 League of Legends 게임 파티를 모집하고 관리하기 위한 Discord Bot입니다.

사용자가 게임 닉네임을 등록한 뒤 원하는 게임 모드의 파티를 생성하거나 참여할 수 있으며,
내전 기능을 통해 팀 구성과 경기 진행까지 관리할 수 있습니다.

---

## 📌 프로젝트 소개

**두두봇**은 Discord 서버 내에서 반복적으로 이루어지는 League of Legends 파티 모집 과정을 간편하게 관리하기 위해 개발한 Discord Bot입니다.

기존의 텍스트 기반 파티 모집 방식에서 발생하는 불편함을 줄이고,

* 파티 생성 및 참여
* 모집 인원 관리
* 게임 닉네임 관리
* 파티 마감 시간 관리
* 내전 팀 구성
* 내전 경기 관리

등을 Discord 내부에서 처리할 수 있도록 구현했습니다.

---

## ✨ 주요 기능

### 🎮 파티 모집

* 증명의 전장 / 협곡 파티 생성
* 일반 / 랭크 게임 모드 지원
* 파티 모집 인원 관리
* 파티 참여 및 참여 취소
* 파티 자동 마감
* 파티 설명 작성
* 참여자 전적 표시
* 모집 인원 초과 시 대기 인원 관리

### 👤 사용자 관리

* Discord 사용자별 게임 닉네임 등록
* 게임 닉네임 변경
* 닉네임 기반 사용자 식별
* 파티 참여 시 등록된 닉네임 사용

### ⚔️ 내전

* 내전 생성 및 시작 시간 설정
* 팀 개수 설정
* 팀별 참가자 관리
* 팀 변경 및 팀 탈퇴
* 내전 모집 상태 관리
* 내전 시작 및 종료 상태 관리
* 팀별 참가자 정보 표시
* 경기 결과 관리

### 🖥️ Discord UI

Discord의 다양한 Interaction 기능을 활용하여 대부분의 작업을 버튼과 Select Menu, Modal을 통해 처리합니다.

* Slash Command
* Button
* Select Menu
* Modal
* Embed

---

## 🛠 기술 스택

### Backend

* Java 21
* Spring Boot 3.3.5
* JDA 5.2.3
* Gradle

### Database

* SQLite
* SQLite JDBC

### Infrastructure

* Google Cloud Platform
* Compute Engine
* Ubuntu
* systemd

### Development

* Git / GitHub
* IntelliJ IDEA

---

## 📂 프로젝트 구조

```text
src
└── main
    └── java
        └── com.dudu
            ├── DuduBot.java
            │
            ├── command
            │
            ├── handler
            │   ├── PartyHandler.java
            │   └── InHouseHandler.java
            │
            ├── dao
            │   ├── MemberDAO.java
            │   ├── PartyDAO.java
            │   ├── InHouseDAO.java
            │   └── InHouseMatchDAO.java
            │
            ├── dto
            │   ├── InHouseDTO.java
            │   ├── InHouseTeamDTO.java
            │   └── InHouseMatchDTO.java
            │
            └── builder
                └── PartyEmbedBuilder.java
```

※ 실제 패키지 구조에 맞게 수정하면 됩니다.

---

## 🎮 사용 방법

### 1. 파티 생성

Discord에서 다음 명령어를 사용합니다.

```text
/증바람
```

게임 닉네임과 파티 설명을 입력하여 파티를 생성할 수 있습니다.

### 2. 파티 참여

생성된 파티의 **참여하기** 버튼을 통해 파티에 참여할 수 있습니다.

참여자는 Embed에 표시되며 모집 인원에 따라 대기 인원으로 분류됩니다.

### 3. 파티 참여 취소

**참여취소** 버튼을 통해 파티에서 나갈 수 있습니다.

### 4. 내전

내전 모집 게시글에서 내전 생성 및 팀 구성 기능을 사용할 수 있습니다.

내전 참가자는 원하는 팀을 선택하거나 다른 팀으로 변경할 수 있으며,
내전 관리자는 모집 상태와 경기 진행 상태를 관리할 수 있습니다.

---

## 🗄️ 데이터베이스

SQLite를 사용하여 별도의 데이터베이스 서버 없이 데이터를 관리합니다.

주요 테이블은 다음과 같습니다.

### `member`

Discord 사용자와 League of Legends 게임 닉네임 및 전적 정보를 관리합니다.

### `party`

게임 파티의 생성자, 메시지 ID, 생성 시간, 마감 시간, 게임 종류 및 모집 상태 등을 관리합니다.

### `in_house`

내전의 시작 시간, 최대 인원, 팀 수 및 진행 상태를 관리합니다.

### `in_house_team`

내전에 생성된 팀 정보를 관리합니다.

### `in_house_match`

내전 경기 및 팀별 경기 결과를 관리합니다.

---

## 🚀 배포

현재 두두봇은 Google Cloud Platform의 Compute Engine VM에서 실행할 수 있도록 구성되어 있습니다.

```text
GitHub
   ↓
GCP Compute Engine
   ↓
Ubuntu
   ↓
Java 21
   ↓
Spring Boot / JDA
   ↓
Discord
```

systemd를 사용하여 서버가 종료되거나 프로세스에 문제가 발생했을 때 봇을 자동으로 다시 실행하도록 구성했습니다.

---

## 🔐 환경 변수

Discord Bot Token과 같은 민감한 정보는 GitHub에 직접 저장하지 않고 환경 변수로 관리합니다.

예:

```env
DISCORD_TOKEN=your_discord_bot_token
```

실제 Token은 `.env` 파일 또는 서버 환경 변수에 저장하며 GitHub에는 업로드하지 않습니다.

---

## 📸 실행 화면

> 실제 Discord 실행 화면을 추가할 예정입니다.

### 파티 모집

![Party](이미지_주소)

### 내전 팀 구성

![InHouse](이미지_주소)

### 내전 관리

![Management](이미지_주소)

---

## 📝 개발 과정

이 프로젝트는 Discord 서버에서 실제로 사용하기 위한 목적으로 개발하면서 기능을 단계적으로 추가했습니다.

초기에는 단순한 파티 모집 기능으로 시작했으며 이후 사용자 관리, 파티 마감, 대기 인원 관리, 내전 팀 구성 및 경기 관리 기능을 추가했습니다.

특히 Discord의 Interaction을 활용하여 별도의 웹 페이지 없이 Discord 내부에서 대부분의 기능을 사용할 수 있도록 구현했습니다.

---

## 🔮 향후 개선 사항

* [ ] 내전 경기 결과 자동 관리
* [ ] 내전 전적 기록
* [ ] 경기 결과에 따른 사용자 전적 반영
* [ ] 관리자 기능 개선
* [ ] 예외 처리 및 오류 메시지 개선
* [ ] 테스트 코드 추가
* [ ] Discord 명령어 및 UI 개선

---

## 👤 개발자

**Jh**

GitHub: [GitHub 프로필 링크]

---

## 📄 License

This project is for personal and educational purposes.
