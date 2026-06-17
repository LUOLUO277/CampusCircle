<template>
  <view class="page">
    <view class="form-card">
      <text class="title">{{ pageTitle }}</text>

      <view class="field">
        <text class="label">{{ text.titleLabel }}</text>
        <textarea
          class="input title-textarea"
          :value="form.title"
          :placeholder="text.titlePlaceholder"
          maxlength="80"
          auto-height
          @input="onTitleInput"
        ></textarea>
      </view>

      <view class="field">
        <text class="label">{{ text.descriptionLabel }}</text>
        <textarea
          class="textarea"
          v-model="form.description"
          :placeholder="text.descriptionPlaceholder"
          maxlength="500"
        ></textarea>
      </view>

      <view class="field">
        <text class="label">{{ text.dateLabel }}</text>
        <picker mode="date" :value="form.deadlineDate" @change="onDateChange">
          <view class="picker">{{ form.deadlineDate || text.datePlaceholder }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="label">{{ text.timeLabel }}</text>
        <picker mode="time" :value="form.deadlineTime" @change="onTimeChange">
          <view class="picker">{{ form.deadlineTime || text.timePlaceholder }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="label">{{ text.typeLabel }}</text>
        <picker :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="picker">{{ currentTypeLabel }}</view>
        </picker>
      </view>

      <view class="field">
        <text class="label">{{ text.previewLabel }}</text>
        <view class="preview">{{ deadlinePreview }}</view>
      </view>

      <button class="save-btn" :loading="saving" @click="submit">{{ text.saveButton }}</button>
    </view>
  </view>
</template>

<script>
import { createScheduleItem, getScheduleItemDetail, updateScheduleItem } from '@/api/info-center'

const TEXT = {
  createTitle: '\u65b0\u5efa\u65e5\u7a0b',
  editTitle: '\u7f16\u8f91\u65e5\u7a0b',
  titleLabel: '\u6807\u9898',
  titlePlaceholder: '\u4f8b\u5982\uff1a\u6570\u636e\u5e93\u4f5c\u4e1a\u63d0\u4ea4',
  descriptionLabel: '\u63cf\u8ff0',
  descriptionPlaceholder: '\u8865\u5145\u8bf4\u660e\u3001\u94fe\u63a5\u6216\u5907\u6ce8',
  dateLabel: '\u622a\u6b62\u65e5\u671f',
  datePlaceholder: '\u8bf7\u9009\u62e9\u65e5\u671f',
  timeLabel: '\u622a\u6b62\u65f6\u95f4',
  timePlaceholder: '\u8bf7\u9009\u62e9\u65f6\u95f4',
  typeLabel: '\u7c7b\u578b',
  previewLabel: '\u622a\u6b62\u9884\u89c8',
  previewPlaceholder: '\u8bf7\u5148\u9009\u62e9\u622a\u6b62\u65e5\u671f\u548c\u65f6\u95f4',
  saveButton: '\u4fdd\u5b58',
  titleRequired: '\u8bf7\u8f93\u5165\u6807\u9898',
  dateRequired: '\u8bf7\u9009\u62e9\u622a\u6b62\u65e5\u671f',
  saveSuccess: '\u4fdd\u5b58\u6210\u529f'
}

const TYPE_OPTIONS = [
  { value: 'ASSIGNMENT', label: '\u4f5c\u4e1a' },
  { value: 'EXAM', label: '\u8003\u8bd5' },
  { value: 'SIGNUP', label: '\u62a5\u540d' },
  { value: 'MEETING', label: '\u4f1a\u8bae' },
  { value: 'PROJECT', label: '\u9879\u76ee' },
  { value: 'OTHER', label: '\u5176\u4ed6' }
]

function createDefaultForm() {
  return {
    id: '',
    title: '',
    description: '',
    deadlineDate: '',
    deadlineTime: '23:59',
    type: 'OTHER'
  }
}

export default {
  data() {
    return {
      text: TEXT,
      typeOptions: TYPE_OPTIONS,
      saving: false,
      form: createDefaultForm()
    }
  },
  computed: {
    pageTitle() {
      return this.form.id ? this.text.editTitle : this.text.createTitle
    },
    currentTypeLabel() {
      return this.typeOptions.find(item => item.value === this.form.type)?.label || TYPE_OPTIONS[5].label
    },
    deadlinePreview() {
      if (!this.form.deadlineDate) {
        return this.text.previewPlaceholder
      }
      return `${this.form.deadlineDate} ${this.form.deadlineTime || '23:59'}`
    }
  },
  onLoad(options) {
    if (options.id) {
      this.form.id = options.id
      this.loadDetail()
    }
  },
  methods: {
    async loadDetail() {
      const res = await getScheduleItemDetail(this.form.id)
      if (res.code !== 200 || !res.data) return

      const raw = `${res.data.deadlineAt || ''}`.replace('T', ' ')
      this.form = {
        id: res.data.id || '',
        title: res.data.title || '',
        description: res.data.description || '',
        deadlineDate: raw.slice(0, 10),
        deadlineTime: raw.slice(11, 16) || '23:59',
        type: res.data.type || 'OTHER'
      }
    },
    onDateChange(event) {
      this.form.deadlineDate = event.detail.value
    },
    onTitleInput(event) {
      this.form.title = `${event.detail.value || ''}`.replace(/[\r\n]+/g, ' ')
    },
    onTimeChange(event) {
      this.form.deadlineTime = event.detail.value
    },
    onTypeChange(event) {
      const selected = this.typeOptions[event.detail.value]
      this.form.type = selected ? selected.value : 'OTHER'
    },
    async submit() {
      if (!this.form.title.trim()) {
        uni.showToast({ title: this.text.titleRequired, icon: 'none' })
        return
      }

      if (!this.form.deadlineDate) {
        uni.showToast({ title: this.text.dateRequired, icon: 'none' })
        return
      }

      const payload = {
        title: this.form.title.trim(),
        description: (this.form.description || '').trim(),
        deadlineAt: `${this.form.deadlineDate}T${this.form.deadlineTime || '23:59'}`,
        type: this.form.type
      }

      this.saving = true
      try {
        const res = this.form.id
          ? await updateScheduleItem(this.form.id, payload)
          : await createScheduleItem(payload)

        if (res.code === 200) {
          uni.showToast({ title: this.text.saveSuccess, icon: 'success' })
          setTimeout(() => {
            uni.navigateBack({ delta: 1 })
          }, 500)
        }
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 30rpx;
  background:
    radial-gradient(circle at top left, rgba(186, 162, 213, 0.18), transparent 26%),
    linear-gradient(180deg, #faf7f2 0%, #f5f1eb 100%);
}

.form-card {
  padding: 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.92);
  border: 1rpx solid rgba(140, 128, 216, 0.12);
  box-shadow: var(--theme-shadow-soft);
}

.title,
.label {
  display: block;
}

.title {
  font-size: 34rpx;
  font-weight: 800;
  color: #0f172a;
}

.field {
  margin-top: 24rpx;
}

.label {
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #475569;
}

.input,
.textarea,
.picker,
.preview {
  display: block;
  width: 100%;
  box-sizing: border-box;
  background: #f8fafc;
  border-radius: 22rpx;
  padding: 22rpx;
  font-size: 26rpx;
  color: #0f172a;
}

.input,
.picker,
.preview {
  min-height: 88rpx;
}

.title-textarea {
  height: 88rpx;
  line-height: 44rpx;
  overflow: hidden;
}

.textarea {
  min-height: 200rpx;
}

.preview {
  color: #64748b;
}

.save-btn {
  margin-top: 30rpx;
  border-radius: 999rpx;
  background: var(--theme-gradient-strong);
  color: #fff;
  font-size: 26rpx;
}

.save-btn::after {
  border: none;
}
</style>
