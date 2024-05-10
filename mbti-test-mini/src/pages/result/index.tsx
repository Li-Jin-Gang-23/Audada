import { View, Image } from "@tarojs/components";
import { AtButton } from "taro-ui";
import "taro-ui/dist/style/components/button.scss"; // 按需引入
import Taro from "@tarojs/taro";
import "./index.scss";
import GlobalFooter from "../../components/GlobalFooter";
import questionResults from "../../data/question_results.json";
import headerBg from "../../assets/headerBg.jpg";
import questions from "../../data/questions.json";
import { getBestQuestionResult } from "../../utils/bizUtils";

/**
 * 测试结果页面
 */
export default () => {
  // 从本地缓存中获取答案列表
  const answerList = Taro.getStorageSync("answerList");
  if (!answerList || answerList.length < 0) {
    // 提示信息
    Taro.showToast({
      title: "答案为空",
      icon: "error",
      duration: 3000,
    });
  }
  // 计算测试结果
  const result = getBestQuestionResult(answerList, questions, questionResults);

  return (
    <View className="indexPage">
      <View className="at-article__h1 title">{result.resultName}</View>
      <View className="at-article__h2 subTitle">{result.resultDesc}</View>
      <AtButton
        type="primary"
        circle
        className="enterBtn"
        onClick={() => {
          Taro.reLaunch({
            url: "/pages/index/index",
          });
        }}
      >
        返回主页
      </AtButton>
      <Image src={headerBg} className="headerBg"></Image>
      <GlobalFooter />
    </View>
  );
};
