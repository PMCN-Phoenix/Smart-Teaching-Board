<template>
    <div>
      <h1>我的课堂</h1>
      <button @click="createNew">创建新课堂</button>
      <ul>
        <li v-for="c in classroomStore.classrooms" :key="c.id">
          {{ c.title }}
          <router-link :to="'/board/' + c.id">进入</router-link>
          <router-link :to="'/history/' + c.id">历史</router-link>
        </li>
      </ul>
    </div>
  </template>
  
  <script setup>
  import { useClassroomStore } from '../stores/classroom'
  import { onMounted } from 'vue'
  
  const classroomStore = useClassroomStore()
  
  onMounted(() => {
    classroomStore.fetchClassrooms()
  })
  
  async function createNew() {
    const title = prompt('课堂标题')
    const desc = prompt('课堂描述')
    if (title) {
      await classroomStore.createClassroom(title, desc || '')
    }
  }
  </script>