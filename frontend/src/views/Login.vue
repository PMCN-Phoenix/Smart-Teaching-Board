<template>
  <div>
    <h1>登录</h1>
    <input v-model="username" placeholder="用户名" />
    <input v-model="password" type="password" placeholder="密码" />
    <button @click="handleLogin">登录</button>
    <router-link to="/register">去注册</router-link>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const password = ref('')

async function handleLogin() {
  try {
    await auth.login({ username: username.value, password: password.value })
    router.push('/dashboard')
  } catch (e) {
    alert('登录失败')
  }
}
</script>