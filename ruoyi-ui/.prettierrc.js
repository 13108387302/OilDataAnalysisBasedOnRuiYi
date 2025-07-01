module.exports = {
  // ===== 基础配置 =====
  // 使用单引号
  singleQuote: true,
  // 不使用分号
  semi: true,
  // 缩进使用2个空格
  tabWidth: 2,
  // 使用空格而不是tab
  useTabs: false,
  // 行尾不使用逗号
  trailingComma: 'none',
  // 对象字面量的大括号间使用空格
  bracketSpacing: true,
  // JSX标签的反尖括号需要换行
  bracketSameLine: false,
  // 箭头函数参数括号：avoid - 能省略括号的时候就省略
  arrowParens: 'avoid',
  // 每行最大长度
  printWidth: 100,
  // 换行符使用 lf
  endOfLine: 'lf',
  // HTML空格敏感性
  htmlWhitespaceSensitivity: 'css',
  // Vue文件中的script和style标签缩进
  vueIndentScriptAndStyle: false,
  // 嵌入式语言格式化
  embeddedLanguageFormatting: 'auto',
  
  // ===== 文件特定配置 =====
  overrides: [
    {
      files: '*.vue',
      options: {
        // Vue文件使用双引号
        singleQuote: false,
        // Vue模板中的属性换行
        htmlWhitespaceSensitivity: 'ignore'
      }
    },
    {
      files: '*.json',
      options: {
        // JSON文件使用双引号
        singleQuote: false,
        // JSON文件不使用尾随逗号
        trailingComma: 'none'
      }
    },
    {
      files: '*.md',
      options: {
        // Markdown文件行长度
        printWidth: 80,
        // Markdown文件保持原有换行
        proseWrap: 'preserve'
      }
    },
    {
      files: '*.scss',
      options: {
        // SCSS文件使用单引号
        singleQuote: true,
        // SCSS文件缩进
        tabWidth: 2
      }
    },
    {
      files: '*.css',
      options: {
        // CSS文件使用单引号
        singleQuote: true
      }
    }
  ]
};
