/* ============================================================
   AI RAG ChatBot — Simulated Response Data
   ============================================================ */

export const aiResponses = {
  summarize: {
    text: `I'd be happy to help you summarize a document! Here's how you can use me effectively for summarization:\n\nJust paste your text or describe the document, and I'll extract the key points for you. I use RAG (Retrieval-Augmented Generation) to understand context and provide accurate summaries.\n\nHere's an example of how I structure summaries:`,
    code: {
      lang: 'markdown',
      content: `# Document Summary\n\n## Key Points\n- Main argument or thesis statement\n- Supporting evidence and data points\n- Notable conclusions or recommendations\n\n## Critical Insights\n- Patterns identified across sections\n- Gaps or areas needing further research\n\n## Action Items\n1. Review highlighted sections\n2. Cross-reference cited sources\n3. Validate statistical claims`
    }
  },
  code: {
    text: `Certainly! Here's a Python script that demonstrates a clean data processing pipeline. It reads CSV data, performs transformations, and outputs results with proper error handling.`,
    code: {
      lang: 'python',
      content: `import pandas as pd\nfrom pathlib import Path\n\ndef process_data(input_path, output_path="output.csv"):\n    """\n    Reads, cleans, and transforms CSV data.\n    Returns processed DataFrame.\n    """\n    try:\n        # Read the source data\n        df = pd.read_csv(input_path)\n        print(f"Loaded {len(df)} rows from {input_path}")\n\n        # Clean: drop nulls, normalize columns\n        df = df.dropna(subset=["id", "value"])\n        df.columns = [c.lower().strip() for c in df.columns]\n\n        # Transform: add computed fields\n        df["value_normalized"] = (\n            df["value"] / df["value"].max()\n        )\n        df["category"] = df["value_normalized"].apply(\n            lambda x: "high" if x > 0.7 else "low"\n        )\n\n        # Export results\n        df.to_csv(output_path, index=False)\n        print(f"Saved {len(df)} rows → {output_path}")\n        return df\n\n    except FileNotFoundError:\n        print(f"Error: {input_path} not found.")\n    except pd.errors.EmptyDataError:\n        print("Error: File is empty.")\n\n# Usage\nresult = process_data("sales_data.csv")`
    }
  },
  analyze: {
    text: `Absolutely! Data analysis is one of my core strengths. Here's a practical example using Python's pandas and numpy to analyze a dataset and extract insights.\n\nThis script calculates summary statistics, detects outliers, and identifies correlations:`,
    code: {
      lang: 'python',
      content: `import pandas as pd\nimport numpy as np\n\ndef analyze_dataset(df, target_col):\n    """\n    Performs comprehensive analysis on a DataFrame.\n    Returns a summary dict with key insights.\n    """\n    insights = {}\n\n    # Basic statistics\n    insights["count"] = len(df)\n    insights["mean"] = df[target_col].mean()\n    insights["median"] = df[target_col].median()\n    insights["std_dev"] = df[target_col].std()\n\n    # Outlier detection (IQR method)\n    Q1 = df[target_col].quantile(0.25)\n    Q3 = df[target_col].quantile(0.75)\n    IQR = Q3 - Q1\n    outliers = df[\n        (df[target_col] < Q1 - 1.5 * IQR) |\n        (df[target_col] > Q3 + 1.5 * IQR)\n    ]\n    insights["outlier_count"] = len(outliers)\n    insights["outlier_pct"] = round(\n        len(outliers) / len(df) * 100, 2\n    )\n\n    # Top correlations\n    numeric = df.select_dtypes(include=[np.number])\n    corr = numeric.corr()[target_col].drop(target_col)\n    insights["top_correlation"] = corr.abs().idxmax()\n\n    return insights`
    }
  }
};

export const genericResponses = [
  {
    text: `That's an interesting question! Let me break it down for you.\n\nBased on my analysis, here are the key points to consider:\n\n1. **Context matters** — The answer depends on several factors specific to your use case\n2. **Best practices** — Following established patterns will save you time and reduce errors\n3. **Iteration** — Start with a minimal approach and refine based on feedback\n\nWould you like me to dive deeper into any specific aspect of this?`,
    code: null
  },
  {
    text: `Great question! Here's what I can tell you about this topic.\n\nThe RAG (Retrieval-Augmented Generation) approach allows me to provide more accurate and contextual responses by combining large language model capabilities with real-time information retrieval.\n\nFor your specific query, I'd recommend:\n\n• Start by clearly defining your requirements\n• Break the problem into smaller, manageable parts\n• Test and validate each component independently\n• Iterate based on results\n\nLet me know if you'd like a more detailed explanation or a code example!`,
    code: null
  },
  {
    text: `I'd be happy to help with that! Let me put together a comprehensive response.\n\nHere's my analysis:\n\n**Overview**\nThis is a well-structured problem that can be approached systematically. The key insight is to leverage modern tools and methodologies to achieve optimal results.\n\n**Recommendations**\n• Use a modular architecture for better maintainability\n• Implement proper error handling and logging\n• Write tests to ensure reliability\n• Document your approach for future reference\n\nShall I provide a more detailed walkthrough or code implementation?`,
    code: null
  }
];

export function getResponse(userText) {
  const lower = userText.toLowerCase();

  if (lower.includes('summarize') || lower.includes('summary') || lower.includes('key points')) {
    return aiResponses.summarize;
  }
  if (lower.includes('code') || lower.includes('script') || lower.includes('python') || lower.includes('function') || lower.includes('program')) {
    return aiResponses.code;
  }
  if (lower.includes('analyze') || lower.includes('analysis') || lower.includes('data') || lower.includes('insight')) {
    return aiResponses.analyze;
  }

  return genericResponses[Math.floor(Math.random() * genericResponses.length)];
}

export function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning.';
  if (hour < 17) return 'Good afternoon.';
  return 'Good evening.';
}

export const defaultHistory = [
  'Exponential backoff retry logic',
  'RAG pipeline architecture',
  'Vector database comparison',
];

export const starterCards = [
  {
    id: 'summarize',
    icon: 'description',
    label: 'Summarize',
    prompt: 'Summarize a long document or article into key points.',
    colorClass: 'card-primary',
  },
  {
    id: 'code',
    icon: 'code',
    label: 'Code',
    prompt: 'Write a Python script for data processing.',
    colorClass: 'card-info',
  },
  {
    id: 'analyze',
    icon: 'analytics',
    label: 'Analyze',
    prompt: 'Analyze data sets and extract meaningful insights.',
    colorClass: 'card-success',
  },
];
