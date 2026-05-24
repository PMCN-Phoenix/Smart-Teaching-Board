<template>
    <div>
      <h1>注册</h1>
      <input v-model="username" placeholder="用户名" />
      <input v-model="password" type="password" placeholder="密码" />
      <input v-model="email" placeholder="邮箱" />
      <button @click="handleRegister">注册</button>
      <router-link to="/login">去登录</router-link>
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
  const email = ref('')
  
  async function handleRegister() {
    try {
      await auth.register({ username: username.value, password: password.value, email: email.value })
      router.push('/dashboard')
    } catch (e) {
      alert('注册失败')
    }
  }
  </script>