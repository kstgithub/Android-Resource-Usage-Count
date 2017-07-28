import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageSearcher;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Index extends AnAction {

    private AtomicInteger mCount;
    private Project mProject;

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        mProject = e.getProject();
        VirtualFile file = e.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
        if (ResourceUsageCountUtils.isTargetFileToCount(file)) {
            findXmlTagFromFile(file);
        }
    }

    private void findXmlTagFromFile(VirtualFile file) {
        PsiFile psiFile = PsiUtilBase.getPsiFile(mProject, file);
        psiFile.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
                if (ResourceUsageCountUtils.isTargetTagToCount(element)) {
                    findTagUsage((XmlTag)element);
                }
            }
        });
    }

    public int findTagUsage(XmlTag element) {
        FindUsagesHandler handler = FindUsageUtils.getFindUsagesHandler(element, mProject);
        if (handler != null) {
            FindUsagesOptions findUsagesOptions = handler.getFindUsagesOptions();
            PsiElement2UsageTargetAdapter[] primaryTargets = FindUsageUtils.convertToUsageTargets(Arrays.asList(handler.getPrimaryElements()), findUsagesOptions);
            PsiElement2UsageTargetAdapter[] secondaryTargets = FindUsageUtils.convertToUsageTargets(Arrays.asList(handler.getSecondaryElements()), findUsagesOptions);
            PsiElement2UsageTargetAdapter[] targets = (PsiElement2UsageTargetAdapter[]) ArrayUtil.mergeArrays(primaryTargets, secondaryTargets);
            Factory<UsageSearcher> factory = () -> {
                return FindUsageUtils.createUsageSearcher(primaryTargets, secondaryTargets, handler, findUsagesOptions, (PsiFile)null);
            };
            UsageSearcher usageSearcher = (UsageSearcher)factory.create();
            mCount = new AtomicInteger(0);
            usageSearcher.generate(new Processor<Usage>() {
                @Override
                public boolean process(Usage usage) {
                    if (ResourceUsageCountUtils.isUsefulUsageToCount(usage)) {
                        mCount.incrementAndGet();
                    }
                    ProgressIndicator indicator1 = ProgressWrapper.unwrap(ProgressManager.getInstance().getProgressIndicator());
                    return !indicator1.isCanceled();
                }
            });
            System.out.println("ele: " + element.getText() + " ele: " + element.getName() + " count: " + mCount);
            return mCount.get();
        }
        return 0;
    }

}