window.secukit = window.secukit || {};

var CONTEXT_PATH = "/"
// 배너이미지 파일 경로 assets/images/banner.png (434x64 px)
secukit.config = {
    methodOrder: ['cert', 'dream', 'wooribank', 'samsung-pass', 'payco', 'pass', 'kakao', 'naver'], // 인증 수단 목록 (사이드 메뉴에 나타남)
    randomMethodOrder: false, // true로 설정 시, 인증 수단 목록을 랜덤하게 설정
    defaultMethod: '', // 기본 인증 수단 설정, 빈 문자열("") 설정 시 첫 번째 인증 수단을 기본으로 함
    singleMode: false, // true로 설정 시, 사이드 메뉴를 감춤, 하나의 기본 인증 수단만 허용하고 싶을 경우 사용.
    path: window.CONTEXT_NAME + '/resource/plugin/SecuKitOne/', // 패키지가 위치한 경로
    lang: 'ko', // 언어 설정 ('ko', 'en', 'fn') 현재 'ko'만 사용 가능
    charset: 'utf8', // 문자열 인코딩
    license : 'Y/dFC6fsmJRwYYtWBLfitSBaAikz3WbVpguQirgDLmrMqKeKllo7Dmy0bhvRXswrBEeNZsj4EU+8YfvyBEMWsVMIedrQIK4izxxiM/HAE97oWbDdrkR7hKvfbzjzRe1EZ72hAzpHW339WZcgAFybpzGxvMXgFR/TGM5Rf/FD2/EyLMwE3JL0NlTTq0sbfxhBn8gvS25cizGeZvt2aOlH4nPzbFET1BmfVXZUoJk8JvxS9VrXKkck7hsT21wM3+H9brzjJL6tdSQNCbUOAI7GE/oYqvN4UQ5iAQKcKV+X6JmTd9W6uqzkat0UHho9miKQaeJjcKxSMfBcl2+a040Gtg==', // 발급 라이선스
    debug: true, // 브라우저 Console log 표시 설정
    sideBanner: 'https://tech.signgate.com/demotest/sectigo_ci.png', // 사이드 배너 링크 (113x113 px), 빈문자열 설정 시 표시 안함
    sideBannerMobile: 'https://tech.signgate.com/demotest/sectigo_ci_m.png', // 사이드 배너 링크 (113x113 px), 빈문자열 설정 시 표시 안함
    sideBannerInfo: '한국정보인증 SSL 링크 배너 이미지', // 사이드 배너 이미지 설명 (웹 접근성)
    sideBannerLink: 'https://www.kicassl.com', // 사이드 배너 링크
    jqueryAlias: $,

    /* 화면 색상 설정 (IE는 미지원함으로 기본 색상으로 동작) */
    theme: {
        mainColor: '#1e4bc6', // 요소 선택 등과 관련된 테두리 및 배경색
        textPlaceholderColor: '#b3b3b3', // 입력 요소의 placehoder 글자 색
        textColor: '#221e1f', // 기본 글자색
        focusShadowColor: '#5385f529', // 요소의 포커스를 표시하는 그림자 색
        borderColor: '#cbcbcb', // 요소 테두리 색
        sidebarBgColor: '#172D69', // 사이드바 배경 색
        sidebarItemColor: '#8F9CC1', // 사이드바 아이템 글자 색
        buttonBgColor: '#000000', // 버튼 배경 색
        buttonColor: '#ffffff', // 버튼 글자 색
    },

    /* 인증서 관련 설정  */
    cert: {
        mediaOrder: ['browser', 'hdd', 'usb', /* 4번째 미디어 부터는 확장 매체에 표시 */ 'hsm', 'bio-hsm', 'phone', 'smart'],
        defaultMedia: 'browser', // 기본 미디어 설정, 빈 문자열("") 설정 시 기본 미디어 미설정
        issuers: {}, // 발급자 표시 설정, 변환전 발급자는 모두 소문자로 입력 ex) { kica: "한국정보인증" }
        certPolicyTable: {}, // 인증서 용도 표시 설정,  {"1.2.410.200004.5.2.1.1": "법인(범용)"} (내부 설정은 아래 주석 참조)
        allowedPolicyOIDs: [], // 인증서 허용 정책 설정, 빈 배열 설정 시 모두 허용
        /* Non-ActiveX  관련 설정  */
        nxs: {
            download: undefined, // function () { // TODO donwload}
            ubikeyDownload: 'https://www.ubikey.co.kr/services/service-join.asp',
            installRequired: false, // true로 설정 시, Non-ActiveX 모듈을 반드시 설치해야함
            version: '1.0.6.14', // Non-ActiveX 모듈 요구 버전 설정
            caIP: 'catest.signgate.com', // 인증서 발급 및 관리 기능 호출 시 필요한 설정, CA 주소
            caCmpPort: '4502', // 인증서 발급 및 관리 기능 호출 시 필요한 설정, CMP 통신 포트
            bioTokenP7Message:
                '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', // Non-ActiveX의 지문보안토큰 리스트 (KISA에서 배포)
        },
        html5: {
            cocs: false,
        },
        smartCert: {
            siteDomain: 'https://www.example.com',
        },
    },

    /* 간편인증 관련 설정  */
    pro: {
        requestURL: window.CONTEXT_NAME + '/SepoaSignOne.sux?type=pro&mode=request', // 간편인증 요청 주소
        getResultURL: window.CONTEXT_NAME + '/SepoaSignOne.sux?type=pro&mode=result', // 간편인증 결과 요청 주소
//        pollingURL: 'https://kiapdev.signgate.com/kiap/jsp/', // 게이트웨이 서버 Polling 페이지 주소 (개발)
//        naverURL: 'https://kiapdev.signgate.com/kiap/jsp/nidindex.jsp', // 게이트웨이 서버 Polling 페이지 주소 (개발)
         pollingURL: 'https://kiap.signgate.com/kiap/jsp/', // 게이트웨이 서버 Polling 페이지 주소 (운영)
         naverURL: 'https://kiap.signgate.com/kiap/jsp/nidindex.jsp', // 게이트웨이 서버 Polling 페이지 주소 (운영)
        callbackURL: '', // 간편인증 콜백 주소, 빈 문자열 설정 시 패키지 내의 callback page 호출
        serviceCode: 'KIAP001', //간편인증서 서비스 유형 ("1": 전자서명과 CI, "2": 전자서명, "3":CI)
        intervalMinute: 5, // 간편인증 팝업 유효시간 (최대 및 최소 시간)
        /* 간편인증 이용 동의 설정, key 값(processingCI 등)은 임의로 지정 및 변경 가능하나 중복 불가
           이용동의 항목 추가 및 삭제 가능 (1 ~ 4개 설정 가능) */
        agreement: {
            processingCI: {
                title: '고유식별번호처리 동의',
                content: '',
            },
            serviceUse: {
                title: '서비스 이용 약관 동의',
                content: '',
            },
            disclosureInformation: {
                title: '제3자 정보제공 동의',
                content: '',
            },
        },
    },

    /* mobile Id 관련 설정 */
    mobileId: {
        requestURL: window.CONTEXT_NAME + '/SepoaSignOne.sux?type=mobileId&mode=request', // 모바일 신분증 인증 요청 주소
        getResultURL: window.CONTEXT_NAME + '/SepoaSignOne.sux?type=mobileId&mode=result', // 모바일 신분증 인증 결과 요청 주소
        interface: {
            pc: 'qr', // 모바일 신분증 PC 화면 인터페이스 (QR 코정)
            mobile: 'app2app', // 모바일 신분증 모바일 화면 인터페이스 (app2app 코정)
        },
        qr: {
            expirationTime: 180,
        },
        app2app: {
            ios: 'MobileID://verify?data_type=byte&mode=direct&data=', // app2app iOS URL scheme 주소
            android: 'mobileid://verify?data_type=byte&mode=direct&data=', // app2app AOS URL scheme 주소
        },
        agreement: {
            processingCI: {
                title: '고유식별번호처리 동의',
                content: '',
            },
            serviceUse: {
                title: '서비스 이용 약관 동의',
                content: '',
            },
        },
    },
};

/*/ 한국정보인증
"1.2.410.200004.5.2.1.1": "법인(범용)",
"1.2.410.200004.5.2.1.2": "개인(범용)",
"1.2.410.200004.5.2.1.7.1": "은행거래용/보험용",
"1.2.410.200004.5.2.1.7.2": "증권거래용/보험용",
"1.2.410.200004.5.2.1.7.3": "신용카드용",

//한국 증권 전산
"1.2.410.200004.5.1.1.7": "법인 범용",
"1.2.410.200004.5.1.1.5": "개인 범용",
"1.2.410.200004.5.1.1.9": "용도제한용",

// 금융결재원
"1.2.410.200005.1.1.5": "법인 범용",
"1.2.410.200005.1.1.1": "개인 범용",
"1.2.410.200005.1.1.4": "은행/보험용",
"1.2.410.200005.1.1.6.2": "신용카드용",

//한국전자인증
"1.2.410.200004.5.4.1.2": "법인 범용",
"1.2.410.200004.5.4.1.1": "개인 범용",
"1.2.410.200004.5.4.1.101": "인터넷뱅킹용",
"1.2.410.200004.5.4.1.102": "증권거래용",
"1.2.410.200004.5.4.1.103": "인터넷보험용",
"1.2.410.200004.5.4.1.104": "신용카드용",
"1.2.410.200004.5.4.1.105": "전자민원용",
"1.2.410.200004.5.4.1.106": "인터넷뱅킹용/전자민원용",
"1.2.410.200004.5.4.1.107": "증권거래용/전자민원용",
"1.2.410.200004.5.4.1.108": "인터넷보험용/전자민원용",
"1.2.410.200004.5.4.1.109": "신용카드용/전자민원용",

//한국무역통신
"1.2.410.200012.1.1.3": "법인 범용",
"1.2.410.200012.11.31": "은행거래용(서명용)",
"1.2.410.200012.11.32": "은행거래용(암호용)",
"1.2.410.200012.11.35": "증권거래용(서명용)",
"1.2.410.200012.11.36": "증권거래용(암호용)",
"1.2.410.200012.11.39": "보험거래용(서명용)",
"1.2.410.200012.11.40": "보험거래용(암호용)",
"1.2.410.200012.11.43": "신용카드용(서명용)",
"1.2.410.200012.11.44": "신용카드용(암호용)",
*/
