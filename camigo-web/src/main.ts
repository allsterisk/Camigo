import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { pinia } from './store'
import './assets/styles/main.css'

const app = createApp(App)

// Pinia must be installed before Router because route guards read the user store.
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
