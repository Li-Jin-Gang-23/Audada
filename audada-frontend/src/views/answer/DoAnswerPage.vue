<template>
  <div id="doAnswerPage">
    <a-card>
      <h1>{{ app.appName }}</h1>
      <p>{{ app.appDesc }}</p>
      <h2 style="margin-bottom: 16px">
        {{ current }}、{{ currentQuestion.title }}
      </h2>
      <div>
        <a-radio-group
          v-model="currentAnswer"
          direction="vertical"
          :options="questionOptions"
          @change="onRadioChange"
          size="large"
        />
      </div>
      <div style="margin-top: 24px">
        <a-space size="large">
          <a-button
            type="primary"
            circle
            v-if="current < questionContent.length"
            :disabled="!currentAnswer"
            @click="current += 1"
          >
            下一题
          </a-button>
          <a-button
            type="primary"
            v-if="current === questionContent.length"
            circle
            :disabled="!currentAnswer"
            :loading="submitting"
            @click="doSubmit"
          >
            {{ submitting ? "评分中" : "查看结果" }}
          </a-button>
          <a-button v-if="current > 1" circle @click="current -= 1">
            上一题
          </a-button>
        </a-space>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {
  withDefaults,
  defineProps,
  ref,
  watchEffect,
  reactive,
  computed,
} from "vue";
import API from "@/api";
import { useRouter } from "vue-router";
import { listQuestionVoByPageUsingPost } from "@/api/questionController";
import message from "@arco-design/web-vue/es/message";
import { getAppVoByIdUsingGet } from "@/api/appController";
import { addUserAnswerUsingPost } from "@/api/userAnswerController";

interface Props {
  appId: string;
}

const props = withDefaults(defineProps<Props>(), {
  appId: () => "",
});
const router = useRouter();
const submitting = ref(false);
const app = ref<API.AppVO>({});
const questionContent = ref([] as API.QuestionContentDTO[]);

// 当前题目序号（从 1 开始）
const current = ref(1);
// 当前题目
const currentQuestion = ref<API.QuestionContentDTO>({});
const questionOptions = computed(() => {
  return currentQuestion.value?.options
    ? currentQuestion.value.options.map((option) => ({
        label: `${option.key}. ${option.value}`,
        value: option.key,
      }))
    : [];
});

// 当前答案
const currentAnswer = ref<string>();
// 回答列表
const answerList = reactive<string[]>([]);

// 加载数据
const loadData = async () => {
  if (!props.appId) {
    return;
  }
  // 获取app
  let res: any = await getAppVoByIdUsingGet({
    id: props.appId as any,
  });
  if (res.data.code === 0 && res.data.data) {
    app.value = res.data.data;
  } else {
    message.error("获取应用失败，" + res.data.message);
  }
  // 获取题目
  res = await listQuestionVoByPageUsingPost({
    appId: props.appId as any,
    current: 1,
    pageSize: 1,
    sortField: "createTime",
    sortOrder: "descend",
  });
  if (res.data.code === 0 && res.data.data?.records) {
    questionContent.value = res.data.data.records[0].questionContent;
  } else {
    message.error("获取题目失败，" + res.data.message);
  }
};

// 记录回答
const onRadioChange = (value: string) => {
  currentAnswer.value = value;
  answerList[current.value - 1] = value;
};

// 获取旧数据
watchEffect(() => {
  loadData();
});
watchEffect(() => {
  currentQuestion.value = questionContent.value[current.value - 1];
  currentAnswer.value = answerList[current.value - 1];
});

/**
 * 提交
 */
const doSubmit = async () => {
  if (!props.appId || !answerList) {
    return;
  }
  submitting.value = true;
  const res = await addUserAnswerUsingPost({
    appId: props.appId as any,
    choices: answerList,
  });
  if (res.data.code === 0 && res.data.data) {
    router.push(`/answer/result/${res.data.data}`);
  } else {
    message.error("提交答案失败，" + res.data.message);
  }
  submitting.value = false;
};
</script>
