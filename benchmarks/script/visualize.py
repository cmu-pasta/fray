import pandas
import seaborn as sns
import matplotlib.pyplot as plt

def visualize_report(path: str, name: str):
    df = pandas.read_csv(path, header=0)
    df['operation'] = df['operation'].apply(lambda x: x.split('.')[-1])
    sns.countplot(data=df, y='operation')
    plt.title(f'Distribution of Operations: {name}')
    plt.xlabel('Count')
    plt.ylabel('Operation')
    plt.show()