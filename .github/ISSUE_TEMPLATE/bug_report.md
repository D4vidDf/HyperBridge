---
name: 🐛 Bug Report
about: Create a report to help us improve HyperBridge
title: "[BUG] "
labels: ["type: bug"]
assignees:
  - D4vidDf
body:
  - type: markdown
    attributes:
      value: |
        Thanks for taking the time to fill out this bug report!
        
        **Note:** Please verify you are using a supported version.
        - **Android:** 15 or 16
        - **HyperOS:** 3.0+

  - type: input
    id: device
    attributes:
      label: Device Model
      description: Which phone are you using?
      placeholder: e.g. Xiaomi 15, POCO F7
    validations:
      required: true

  - type: input
    id: os_version
    attributes:
      label: OS Version
      description: Which version of HyperOS are you on? (Min 3.0)
      placeholder: e.g. HyperOS 3.0.1.0
    validations:
      required: true

  - type: dropdown
    id: android_version
    attributes:
      label: Android Version
      options:
        - Android 16
        - Android 15
        - Other
    validations:
      required: true

  - type: dropdown
    id: app_version
    attributes:
      label: App Version
      description: "Which version is causing the issue? (Check tags: https://github.com/D4vidDf/HyperBridge/tags)"
      options:
        - v0.4.0 (Stable)
        - v0.4.0-rc1
        - Dev Build (Please specify in description)
        - Other
    validations:
      required: true

  - type: textarea
    id: description
    attributes:
      label: Describe the Bug
      description: A clear and concise description of what the bug is.
      placeholder: The widget overlay flickers when...
    validations:
      required: true

  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: How can we make this happen on our devices?
      placeholder: |
        1. Go to 'Settings'
        2. Click on 'Design'
        3. ...
    validations:
      required: true

  - type: textarea
    id: logs
    attributes:
      label: Crash Logs (Logcat)
      description: "If the app crashed, these logs are vital."
      render: shell
      placeholder: |
        java.lang.NullPointerException: ...
    validations:
      required: false

  - type: textarea
    id: screenshots
    attributes:
      label: Screenshots / Screen Recording
      description: You can drag and drop images or videos directly into this box.
    validations:
      required: false

  - type: checkboxes
    id: terms
    attributes:
      label: Checklist
      options:
        - label: I have checked that this issue has not already been reported.
          required: true
        - label: I have granted all required permissions (Notification Access, Autostart).
          required: false

---
