<template>
  <AuthShell
    title="创建账号"
    subtitle="从一个想法开始，把灵感逐步变成完整应用"
    variant="register"
  >
    <a-form
      class="auth-form"
      :model="formState"
      name="register"
      layout="vertical"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号">
          <template #prefix>
            <UserOutlined />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码">
          <template #prefix>
            <LockOutlined />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { min: 8, message: '密码不能小于 8 位' },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码">
          <template #prefix>
            <SafetyOutlined />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" block>注册</a-button>
      </a-form-item>
      <div class="tips">
        <span>已有账号？</span>
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
    </a-form>
  </AuthShell>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'
import { LockOutlined, SafetyOutlined, UserOutlined } from '@ant-design/icons-vue'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

/**
 * 验证确认密码
 * @param rule
 * @param value
 * @param callback
 */
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
.tips {
  display: flex;
  justify-content: center;
  gap: 6px;
  color: #64748b;
  font-size: 14px;
}

.tips a {
  color: #1677ff;
  font-weight: 700;
}
</style>
